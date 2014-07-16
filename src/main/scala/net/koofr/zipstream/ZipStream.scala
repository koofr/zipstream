package net.koofr.zipstream

import scala.concurrent.{ExecutionContext, Future, Promise}
import java.util.Date
import java.util.Calendar
import java.util.zip.CRC32
import java.nio.charset.Charset
import play.api.libs.iteratee._
import Utils._

case class ZipInfo(filename: Bytes, date: Int, time: Int, compressType: Int,
    comment: Bytes, extra: Bytes, createSystem: Int, createVersion: Int,
    extractVersion: Int, reserved: Int, flagBits: Int, volume: Int,
    internalAttr: Int, externalAttr: Int, headerOffset: Int, crc: Int,
    compressSize: Int, fileSize: Int) {
  
  def header: Array[Byte] = {
    Array[Byte](0x50, 0x4b, 0x03, 0x04) ++
      extractVersion.littleByte ++
      reserved.littleByte ++
      flagBits.littleShort ++
      compressType.littleShort ++
      time.littleShort ++
      date.littleShort ++
      crc.littleInt ++
      compressSize.littleInt ++
      fileSize.littleInt ++
      filename.length.littleShort ++
      extra.length.littleShort ++
      filename ++
      extra
  }
  
  def footer = {
    crc.littleInt ++
    fileSize.littleInt ++
    compressSize.littleInt
  }
  
  def centDir = {
    val diskNumber = 0
    
    val centDir = Array[Byte](0x50, 0x4b, 0x01, 0x02) ++
      createVersion.littleByte ++
      createSystem.littleByte ++
      extractVersion.littleByte ++
      reserved.littleByte ++
      flagBits.littleShort ++
      compressType.littleShort ++
      time.littleShort ++
      date.littleShort ++
      crc.littleInt ++
      compressSize.littleInt ++
      fileSize.littleInt ++
      filename.length.littleShort ++
      extra.length.littleShort ++
      comment.length.littleShort ++
      diskNumber.littleShort ++
      internalAttr.littleShort ++
      externalAttr.littleInt ++
      headerOffset.littleInt ++
      filename ++
      extra ++
      comment
    
    centDir
  }
  
}

object ZipInfo {
  
  def apply(filename: String, modified: Date, isDir: Boolean): ZipInfo = {
    val nameBytes = filename.getBytes(Charset.forName("UTF8"))
    
    val cal = Calendar.getInstance()
    cal.setTime(modified)
    
    val dt = Seq(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
      cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY),
      cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))
  
    val date = (dt(0) - 1980) << 9 | dt(1) << 5 | dt(2)
    val time = dt(3) << 11 | dt(4) << 5 | (dt(5) / 2)
    
    ZipInfo(
      filename = nameBytes,
      date = date,
      time = time,
      compressType = 0, // store
      comment = Array[Byte](),
      extra = Array[Byte](),
      createSystem = 3, // unix
      createVersion = 20,
      extractVersion = 20,
      reserved = 0,
      flagBits = if (isDir) 0x800 else 0x808, // UTF8 filename, CRC at the end
      volume = 0,
      internalAttr = 0,
      externalAttr = if (isDir) 0x41fd0000 else 0x81b40000,
      headerOffset = 0,
      crc = 0,
      compressSize = 0,
      fileSize = 0
    )
  }
  
}

case class ZipEnd(diskNumber: Int, centDirDisk: Int, centDirDiskCount: Int,
    centDirTotalCount: Int, centDirSize: Int, centDirOffset: Int,
    comment: Bytes) {
  
  def bytes: Array[Byte] = {
    Array[Byte](0x50, 0x4b, 0x05, 0x06) ++
      diskNumber.littleShort ++
      centDirDisk.littleShort ++
      centDirDiskCount.littleShort ++
      centDirTotalCount.littleShort ++
      centDirSize.littleInt ++
      centDirOffset.littleInt ++
      comment.length.littleShort ++
      comment
  }
  
}

object ZipEnd {
  
  def apply(centDirCount: Int, centDirSize: Int, centDirOffset: Int): ZipEnd = {
    ZipEnd(
      diskNumber = 0,
      centDirDisk = 0,
      centDirDiskCount = centDirCount,
      centDirTotalCount = centDirCount,
      centDirSize = centDirSize,
      centDirOffset = centDirOffset,
      comment = Array[Byte]()
    )
  }
  
}

case class ZipFileInfo(name: String, isDir: Boolean, modified: Date,
    getContent: Option[() => Future[Enumerator[Bytes]]])

object ZipStream {

  def apply(files: Seq[ZipFileInfo])(implicit executionContext: ExecutionContext): Enumerator[Bytes] = {
    val (filesEn, infosSizeF) = files.foldLeft((Enumerator[Bytes](), Future.successful((Seq[ZipInfo](), 0)))) { case ((totalEn, infosSizeF), file) =>
      val nextF = infosSizeF map { case (infos, offset) =>
        val inf = ZipInfo.apply(file.name, file.modified, file.isDir)
        
        if (file.isDir) {
          val header = inf.header
          
          offset + header.length
          
          (Enumerator(header), Future.successful((infos :+ inf, offset + header.length)))
        } else {
          val contentEn = Enumerator.flatten(file.getContent.get())
          
          val infCompleteP = Promise[ZipInfo]()
          
          val contentWithFooter = contentEn.foldAndThen((new CRC32, 0)) { case ((crcCalc, size), data) =>
            crcCalc.update(data)
            (crcCalc, size + data.length)
          } { case (crcCalc, totalSize) =>
            val crc = crcCalc.getValue().asInstanceOf[Int]
            
            val infComplete = inf.copy(
              headerOffset = offset,
              crc = crc,
              fileSize = totalSize,
              compressSize = totalSize
            )
            
            infCompleteP.success(infComplete)
            
            Enumerator(infComplete.footer)
          }
          
          val fileEn = Enumerator(inf.header) >>> contentWithFooter
          
          val (fileFoldedEn, totalSizeF) = fileEn.fold(0) { (size, data) =>
            size + data.length
          }
          
          val nextInfosSizeF = infCompleteP.future flatMap { inf =>
            totalSizeF map { totalSize =>
              (infos :+ inf, offset + totalSize)
            }
          }
          
          (fileFoldedEn, nextInfosSizeF)
        }
      }
      
      val nextEn = Enumerator.flatten(nextF.map(_._1))
      val nextInfosSizeF = nextF.flatMap(_._2)
      
      (totalEn >>> nextEn, nextInfosSizeF)
    }
    
    val centDirsWithEndEnF = infosSizeF map { case (infos, filesSize) =>
      val (centDirsEn, centDirsSize) = infos.foldLeft((Enumerator[Bytes](), 0)) { case ((centDirsEn, totalSize), info) =>
        val centDir = info.centDir
        val size = centDir.length
        
        (centDirsEn >>> Enumerator(centDir), totalSize + size)
      }
      
      val end = ZipEnd(infos.length, centDirsSize, filesSize)
      
      val endEn = Enumerator(end.bytes)
      
      centDirsEn >>> endEn
    }
    
    val centDirsWithEndEn = Enumerator.flatten(centDirsWithEndEnF)
    
    filesEn >>> centDirsWithEndEn >>> Enumerator.eof
  }

}

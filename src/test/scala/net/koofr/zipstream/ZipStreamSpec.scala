package net.koofr.zipstream

import language.reflectiveCalls
import scala.concurrent._
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit._
import java.util.Enumeration
import scala.concurrent.ExecutionContext.Implicits._
import scala.collection.JavaConversions._
import java.io.{File, FileOutputStream}
import org.specs2.mutable._
import org.apache.commons.compress.archivers.zip.{ZipFile, ZipArchiveEntry}
    
import play.api.libs.iteratee._

class ZipStreamSpec extends Specification {

  "ZipStream" should {
    "zip files" in {
      val files = Seq(
        ZipFileInfo("file.txt", false, new java.util.Date(1362609357000L),
            Some(() => Future.successful(Enumerator("content".getBytes))))
      )
      
      val zip = Array[Int](
        0x50, 0x4b, 0x3, 0x4,   // Local file header signature
        0x14, 0x0,              // Version needed to extract (minimum)
        0x8, 0x8,               // General purpose bit flag
        0x0, 0x0,               // Compression method
        0x7c, 0xbc,             // File last modification time
        0x66, 0x42,             // File last modification date
        0x0, 0x0, 0x0, 0x0,     // CRC-32
        0x0, 0x0, 0x0, 0x0,     // Compressed size
        0x0, 0x0, 0x0, 0x0,     // Uncompressed size
        0x8, 0x0,               // File name length
        0x0, 0x0,               // Extra field length
        'f', 'i', 'l', 'e', '.', 't', 'x', 't', // File name
                                // Extra field
        'c', 'o', 'n', 't', 'e', 'n', 't', // File content
        0xa9, 0x30, 0xc5, 0xfe, // CRC-32
        0x7, 0x0, 0x0, 0x0,     // Compressed size
        0x7, 0x0, 0x0, 0x0,     // Uncompressed size
        
        0x50, 0x4b, 0x1, 0x2,   // Central directory file header signature
        0x14, 0x3,              // Version made by
        0x14, 0x0,              // Version needed to extract (minimum)
        0x8, 0x8,               // General purpose bit flag
        0x0, 0x0,               // Compression method
        0x7c, 0xbc,             // File last modification time
        0x66, 0x42,             // File last modification date
        0xa9, 0x30, 0xc5, 0xfe, // CRC-32
        0x7, 0x0, 0x0, 0x0,     // Compressed size
        0x7, 0x0, 0x0, 0x0,     // Uncompressed size
        0x8, 0x0,               // File name length
        0x0, 0x0,               // Extra field length
        0x0, 0x0,               // File comment length
        0x0, 0x0,               // Disk number where file starts
        0x0, 0x0,               // Internal file attributes
        0x0, 0x0, 0xb4, 0x81,   // External file attributes
        0x0, 0x0, 0x0, 0x0,     // Relative offset of local file header
        'f', 'i', 'l', 'e', '.', 't', 'x', 't', // File name
        
        0x50, 0x4b, 0x5, 0x6,   // End of central directory signature
        0x0, 0x0,               // Number of this disk
        0x0, 0x0,               // Disk where central directory starts
        0x1, 0x0,               // Number of central directory records on this disk
        0x1, 0x0,               // Total number of central directory records
        0x36, 0x0, 0x0, 0x0,    // Size of central directory (bytes)
        0x39, 0x0, 0x0, 0x0,    // Offset of start of central directory, relative to start of archive
        0x0, 0x0                // Comment length (n)
                                // Comment
      ).map(_.toByte)
      
      val result = zipResult(files)
      
      result must equalTo(zip)
    }
    
    "zip files and folders" in {
      val files = Seq(
        ZipFileInfo("folder/", true, new java.util.Date(), None),
        ZipFileInfo("folder/file.txt", false, new java.util.Date(),
            Some(() => Future.successful(Enumerator("content".getBytes))))
      )
      
      val result = zipResult(files)
      
      result.length must equalTo(237)
    }
    
    "zip files and folders and open" in {
      val files = Seq(
        ZipFileInfo("folder/", true, new java.util.Date(), None),
        ZipFileInfo("folder/file.txt", false, new java.util.Date(),
            Some(() => Future.successful(Enumerator("content".getBytes)))),
        ZipFileInfo("f1.txt", false, new java.util.Date(),
            Some(() => Future.successful(Enumerator("content1".getBytes))))
      )
      
      val result = zipResult(files)
      
      val entries = zipExtract(result)
      
      val (entry1, entry1Content) = entries(0)
      val (entry2, entry2Content) = entries(1)
      val (entry3, entry3Content) = entries(2)
      
      (entries must have size(3))
        .and (entry1.getName must equalTo("folder/"))
        .and (entry1.isDirectory must beTrue)
        
        .and (entry2.getName must equalTo("folder/file.txt"))
        .and (entry2.isDirectory must beFalse)
        .and (entry2Content must equalTo("content"))
        
        .and (entry3.getName must equalTo("f1.txt"))
        .and (entry3.isDirectory must beFalse)
        .and (entry3Content must equalTo("content1"))
    }
  }
  
  def zipResult(files: Seq[ZipFileInfo]) = {
    val stream = ZipStream(files)
    
    val consume = Iteratee.consume[Array[Byte]]()
    
    Await.result(stream(consume).flatMap(i => i.run), Duration(3, SECONDS))
  }
  
  def zipExtract(bytes: Array[Byte]) = {
    val file = File.createTempFile("zipfile", ".zip")
    
    val out = new FileOutputStream(file)
    out.write(bytes)
    out.close()
    
    val zipFile = new ZipFile(file)
    
    val zes = zipFile.getEntries()
    
    def getEntries(zes: Enumeration[ZipArchiveEntry]): List[(ZipArchiveEntry, String)] = {
      if (!zes.hasMoreElements()) {
        Nil
      } else {
        val entry = zes.nextElement()
        
        val content = if (entry.isDirectory) {
          ""
        } else {
          val stream = zipFile.getInputStream(entry)
          val entryBytes = new Array[Byte](entry.getSize.asInstanceOf[Int])
          stream.read(entryBytes)
          new String(entryBytes)
        }
        
        (entry, content) :: getEntries(zes)
      }
    }
    
    val entries = getEntries(zes).toSeq
    
    zipFile.close()
    file.delete()
    
    entries
  }
}

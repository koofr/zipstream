package net.koofr.zipstream

import language.reflectiveCalls

import scala.concurrent.{ExecutionContext, Future, Promise}
import play.api.libs.iteratee._

object Utils {
  
  type Bytes = Array[Byte]
  
  implicit class LittleInt(i: Int) {
    def littleInt = Array[Byte](
      (i & 0xff).asInstanceOf[Byte],
      (i >> 8 & 0xff).asInstanceOf[Byte],
      (i >> 16 & 0xff).asInstanceOf[Byte],
      (i >> 24 & 0xff).asInstanceOf[Byte]
    )
    
    def littleShort = Array[Byte](
      (i & 0xff).asInstanceOf[Byte],
      (i >> 8 & 0xff).asInstanceOf[Byte]
    )
    
    def littleByte = Array[Byte]((i & 0xff).asInstanceOf[Byte])
  }
  
  implicit class EnumeratorUtils[E](en: Enumerator[E]) {
    def fold[S](state: S)(f: (S, E) => S)(implicit executionContext: ExecutionContext) = {
      var st = state
      
      val folder = Enumeratee.map[E] { data =>
        st = f(st, data)
        data
      }
      
      val endStateP = Promise[S]()
      
      val onEof = Enumeratee.onEOF[E] { () =>
        endStateP.success(st)
      }
      
      (en &> folder &> onEof, endStateP.future)
    }
    
    def foldAndThen[S](state: S)(f: (S, E) => S)(endF: S => Enumerator[E])(implicit executionContext: ExecutionContext) = {
      val (foldedEn, endStateF) = fold(state)(f)
      
      val endEn = Enumerator.flatten(endStateF.map(endF))
      
      foldedEn >>> endEn
    }
  }

}
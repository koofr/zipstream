package controllers

import scala.concurrent.Future
import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._
import net.koofr.zipstream._

object Application extends Controller {

  def index = Action {
    val enF = Future.successful(Enumerator("content".getBytes))

    val files = Seq(
      ZipFileInfo("folder/", true, new java.util.Date(), None),
      ZipFileInfo("folder/file.txt", false, new java.util.Date(), Some(() => enF))
    )
    
    val stream = ZipStream(files)
    
    Ok.chunked(stream).withHeaders("content-type" -> "application/zip")
  }

}

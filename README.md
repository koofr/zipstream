# zipstream

zipstream is a Scala library that uses Play framework's iteratees to stream zip files.

## Example

This is an example of Play controller:

    package controllers

    import scala.concurrent.Future
    import play.api._
    import play.api.mvc._
    import play.api.libs.iteratee._
    import net.koofr.zipstream._

    object Application extends Controller {
      def index = Action {
        val enF = Future.successful(Enumerator("content".getBytes))

        val files = Seq(
          ZipFileInfo("folder/", true, new java.util.Date(), None),
          ZipFileInfo("folder/file.txt", false, new java.util.Date(), Some(() => enF))
        )
        
        val stream = ZipStream(files)
        
        Ok.stream(stream).withHeaders("content-type" -> "application/zip")
      }
    }

## Setup

Add following lines to your `build.sbt` or `project/Build.scala` file:

    resolvers += "Koofr Github repo" at "http://koofr.github.com/repo/releases/"

    libraryDependencies += "net.koofr" %% "zipstream" % "0.1"

## Authors

Crafted by highly motivated engineers at http://koofr.net and, hopefully, making your day just a little bit better.

# zipstream

zipstream is a Scala library that uses Play framework's iteratees to stream zip files.

Files in ZIP are NOT compressed. Library can be useful if you want to transfer multiple file at once (download folder). Max file size is 4GB (ZIP64 is not supported yet).

Data source can be any enumerator (length doesn't need to be known in advance).

## Example

This is an example of Play controller:

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

## Setup

Add following lines to your `build.sbt` file:

    resolvers += "Koofr Github repo" at "http://koofr.github.com/repo/maven/"
    
    libraryDependencies += "net.koofr" %% "zipstream" % "0.1"
    
    scalaVersion := "2.10.4"
    
    libraryDependencies += "play" %% "play-iteratees" % "2.1.0" exclude("org.scala-stm", "scala-stm_2.10")
    
    // libraryDependencies += "com.typesafe.play" %% "play-iteratees" % "2.3.1" exclude("org.scala-stm", "scala-stm_2.10")
    
    // uncomment next line if you are using the library without Play framework
    // resolvers += "Typesafe Releases Repository" at "http://repo.typesafe.com/typesafe/releases/"

## Authors

Crafted by highly motivated engineers at http://koofr.net and, hopefully, making your day just a little bit better.

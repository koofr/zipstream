import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "sample"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "net.koofr" %% "zipstream" % "0.1"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "Koofr Github repo" at "http://koofr.github.com/repo/releases/"
  )

}

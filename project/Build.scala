import sbt._
import sbt.Keys._

object ZipstreamBuild extends Build {

  lazy val zipstream = Project(
    id = "zipstream",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "zipstream",
      organization := "net.koofr",
      version := "0.1",
      scalaVersion := "2.10.0",

      resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      
      libraryDependencies ++= Seq(
        "play" %% "play-iteratees" % "2.1.0",
        "org.specs2" %% "specs2" % "1.14" % "test",
        "org.apache.commons" % "commons-compress" % "1.4.1" % "test"
      )
    )
  )
}

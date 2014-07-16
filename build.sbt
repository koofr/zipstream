name := "zipstream"

organization := "net.koofr"

version := "0.2"

scalaVersion := "2.11.1"

crossScalaVersions := Seq("2.10.4", "2.11.1")

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-iteratees" % "2.3.1",
  "org.specs2" %% "specs2" % "2.3.13" % "test",
  "org.apache.commons" % "commons-compress" % "1.4.1" % "test"
)

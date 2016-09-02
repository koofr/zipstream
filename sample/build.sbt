name := """sample"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += "Koofr Github repo" at "http://koofr.github.com/repo/maven/"

libraryDependencies += "net.koofr" %% "zipstream" % "0.2"

scalaVersion := "2.11.1"

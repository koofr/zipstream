name := """sample"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).dependsOn(file(".."))

scalaVersion := "2.11.1"

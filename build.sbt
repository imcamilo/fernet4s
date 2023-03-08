import Dependencies._

ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.github.imcamilo"
ThisBuild / organizationName := "com.github.imcamilo"

lazy val root = (project in file("."))
  .settings(
    name := "fernet4s",
    libraryDependencies ++= Seq(
      munit % Test,
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
      "org.slf4j" % "slf4j-log4j12" % "1.7.30"
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

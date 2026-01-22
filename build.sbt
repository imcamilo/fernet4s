import Dependencies._

// Scala versions
val scala213 = "2.13.14"
val scala3 = "3.3.3"

ThisBuild / scalaVersion := scala213
ThisBuild / crossScalaVersions := Seq(scala213, scala3)
ThisBuild / version := "1.0.0"
ThisBuild / organization := "io.github.imcamilo"
ThisBuild / organizationName := "fernet4s"

// Java 11+
ThisBuild / javacOptions ++= Seq("-source", "11", "-target", "11")

lazy val root = (project in file("."))
  .settings(
    name := "fernet4s",
    libraryDependencies ++= Seq(
      munit % Test,
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
      "io.circe" %% "circe-parser" % "0.14.6" % Test,
      "org.slf4j" % "slf4j-api" % "2.0.12",
      "org.slf4j" % "slf4j-simple" % "2.0.12" % Runtime
    )
  )

// Publishing configuration
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("https://github.com/imcamilo/fernet4s"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/imcamilo/fernet4s"),
    "scm:git@github.com:imcamilo/fernet4s.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "imcamilo",
    name = "Camilo",
    email = "imcamilo@users.noreply.github.com",
    url = url("https://github.com/imcamilo")
  )
)

import Dependencies._

ThisBuild / scalaVersion := "2.13.14"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "io.github.imcamilo"
ThisBuild / organizationName := "fernet4s"

lazy val root = (project in file("."))
  .settings(
    name := "fernet4s",
    libraryDependencies ++= Seq(
      munit % Test,
      "org.scalatest" %% "scalatest" % "3.2.18" % Test,
      "org.slf4j" % "slf4j-api" % "2.0.12",
      "org.slf4j" % "slf4j-simple" % "2.0.12" % Runtime,
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.typelevel" %% "cats-effect" % "3.5.4"
    )
  )

// Publishing configuration
ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

ThisBuild / publishMavenStyle := true
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

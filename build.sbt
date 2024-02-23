import Dependencies._

ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "h1rono"
ThisBuild / organizationName := "h1rono"

fork in run := true

lazy val root = (project in file("."))
  .settings(
    name := "bot-tartaglia",
    // deps
    libraryDependencies += munit % Test,
    libraryDependencies ++= http4s,
    libraryDependencies += circeGeneric,
    // configurations required by scalafix
    semanticdbEnabled := true, // enable SemanticDB
    semanticdbVersion := scalafixSemanticdb.revision, // only required for Scala 2.x
    scalacOptions += "-Ywarn-unused-import", // Scala 2.x only, required by `RemoveUnused`
    // required by http4s
    scalacOptions += "-Ypartial-unification"
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

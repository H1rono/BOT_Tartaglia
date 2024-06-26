import Dependencies._

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "h1rono"
ThisBuild / organizationName := "h1rono"

run / fork := true

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "bot-tartaglia",
    // deps
    libraryDependencies += munit % Test,
    libraryDependencies ++= http4s,
    libraryDependencies += circeGeneric,
    libraryDependencies += logback,
    // configurations required by scalafix
    semanticdbEnabled := true, // enable SemanticDB
    semanticdbVersion := scalafixSemanticdb.revision, // only required for Scala 2.x
    scalacOptions += "-Wunused",
    // Docker configuration
    dockerBaseImage := "eclipse-temurin:21-jre-jammy"
    // TODO: LABEL設定
    // https://docs.github.com/ja/packages/working-with-a-github-packages-registry/working-with-the-container-registry#labelling-container-images
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.

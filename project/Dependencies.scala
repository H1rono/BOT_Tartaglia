import sbt._

object Dependencies {
  lazy val munit = "org.scalameta" %% "munit" % "0.7.29"
  lazy val json4sNative = "org.json4s" %% "json4s-native" % "4.1.0-M4"
  private val http4sVersion = "0.23.25"
  lazy val http4s = Seq(
    "org.http4s" %% "http4s-ember-server" % http4sVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion,
  )
  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.14.6"
}

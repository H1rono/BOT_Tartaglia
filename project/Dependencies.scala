import sbt._

object Dependencies {
  lazy val munit = "org.scalameta" %% "munit" % "0.7.29"
  private val http4sVersion = "0.23.25"
  lazy val http4s = Seq(
    "org.http4s" %% "http4s-ember-client" % http4sVersion,
    "org.http4s" %% "http4s-ember-server" % http4sVersion,
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-circe" % http4sVersion
  )
  lazy val circeGeneric = "io.circe" %% "circe-generic" % "0.14.6"
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.5.0"
}

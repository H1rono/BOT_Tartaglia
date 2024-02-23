package h1rono

import cats.effect.Async
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._
import cats.effect.std.Console

object BotServer {
  def run[F[_]: Async: Network: Console]: F[Nothing] = {
    val helloWorldAlg = HelloWorld.impl[F]
    val dumpReqAlg = DumpReq.impl[F]

    // Combine Service Routes into an HttpApp.
    // Can also be done via a Router if you
    // want to extract segments not checked
    // in the underlying routes.
    val httpApp = (
      BotRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
        BotRoutes.dumpRequestRoutes[F](dumpReqAlg)
    ).orNotFound

    for {
      _ <-
        EmberServerBuilder
          .default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(httpApp)
          .build
    } yield ()
  }.useForever
}

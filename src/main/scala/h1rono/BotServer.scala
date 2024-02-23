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
    val verificationToken = loadEnv("VERIFICATION_TOKEN")
    val accessToken = loadEnv("BOT_ACCESS_TOKEN")

    val helloWorldAlg = HelloWorld.impl[F]
    val dumpReqAlg = DumpReq.impl[F]
    val botHandlerAlg = BotHandler.impl[F]

    // Combine Service Routes into an HttpApp.
    // Can also be done via a Router if you
    // want to extract segments not checked
    // in the underlying routes.
    val httpApp = (
      BotRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
        BotRoutes.dumpRequestRoutes[F](dumpReqAlg) <+>
        BotRoutes.botHandlerRoutes[F](botHandlerAlg)
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

  private def loadEnv(name: String): String =
    sys.env.get(name).getOrElse(throw new Exception(s"env-var $name is not present"))
}

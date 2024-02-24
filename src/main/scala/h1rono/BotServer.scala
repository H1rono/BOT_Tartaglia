package h1rono

import cats.effect.Async
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.implicits._
import cats.effect.std.Console
import cats.data.EitherT
import cats.effect.kernel.Resource
import org.http4s.HttpApp
import cats.effect.std.Env

object BotServer {
  def run[F[_]: Async: Network: Console: Env]: EitherT[F, String, Resource[F, Server]] = for {
    verificationToken <- loadEnvF[F]("VERIFICATION_TOKEN")
    accessToken <- loadEnvF[F]("BOT_ACCESS_TOKEN")
    helloWorldAlg = HelloWorld.impl[F]
    dumpReqAlg = DumpReq.impl[F]
    botHandlerAlg = BotHandler.impl[F](BotHandler.Config(verificationToken, accessToken))

    // Combine Service Routes into an HttpApp.
    // Can also be done via a Router if you
    // want to extract segments not checked
    // in the underlying routes.
    httpApp = (
      BotRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
        BotRoutes.dumpRequestRoutes[F](dumpReqAlg) <+>
        BotRoutes.botHandlerRoutes[F](botHandlerAlg)
    ).orNotFound
    server <- EitherT.rightT(buildServer(httpApp))
  } yield server

  private def buildServer[F[_]: Async](app: HttpApp[F]): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(app)
      .build

  private def loadEnvF[F[_]: Async: Env](name: String): EitherT[F, String, String] = for {
    v <- EitherT.fromOptionF(Env[F].get(name), s"env-var $name is not present")
  } yield v
}

package h1rono

import cats.data.EitherT
import cats.effect.Async
import cats.effect.std.{Console, Env}
import cats.effect.kernel.Resource
import cats.syntax.all._
import com.comcast.ip4s._
import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.implicits._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

object BotServer {
  final case class Config(
      verificationToken: String,
      accessToken: String,
      botId: String,
      botUserId: String
  )

  def config[F[_]: Async: Env]: EitherT[F, String, Config] = for {
    verificationToken <- loadEnvF[F]("VERIFICATION_TOKEN")
    accessToken <- loadEnvF[F]("BOT_ACCESS_TOKEN")
    botId <- loadEnvF("BOT_ID")
    botUserId <- loadEnvF("BOT_USER_ID")
  } yield Config(verificationToken, accessToken, botId, botUserId)

  def run[F[_]: Async: Network: Console](
      conf: Config
  ): F[Nothing] = conf match {
    case Config(verificationToken, accessToken, botId, botUserId) =>
      {
        for {
          baseClient <- buildClient

          traqClient = TraqClient.impl(baseClient, botId, botUserId, accessToken)
          helloWorldAlg = HelloWorld.impl[F]
          dumpReqAlg = DumpReq.impl[F]
          botHandlerAlg = BotHandler.impl[F](BotHandler.Config(traqClient, verificationToken))

          // Combine Service Routes into an HttpApp.
          // Can also be done via a Router if you
          // want to extract segments not checked
          // in the underlying routes.
          httpApp = (
            BotRoutes.helloWorldRoutes[F](helloWorldAlg) <+>
              BotRoutes.dumpRequestRoutes[F](dumpReqAlg) <+>
              BotRoutes.botHandlerRoutes[F](botHandlerAlg)
          ).orNotFound

          server <- buildServer(httpApp)
        } yield ()
      }.useForever
  }

  private def buildClient[F[_]: Async]: Resource[F, Client[F]] =
    EmberClientBuilder.default[F].build

  private def buildServer[F[_]: Async](app: HttpApp[F]): Resource[F, Server] =
    EmberServerBuilder
      .default[F]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(app)
      .build

  private def loadEnvF[F[_]: Async: Env](name: String): EitherT[F, String, String] =
    EitherT.fromOptionF(Env[F].get(name), s"env-var $name is not present")
}

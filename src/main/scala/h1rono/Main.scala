package h1rono

import cats.effect.{IO, IOApp}
import cats.effect.std
import cats.effect.ExitCode

object Main extends IOApp {
  def run(_args: List[String]) = for {
    config <- BotServer.config[IO].value
    code <- config match {
      case Left(exp)     => std.Console[IO].println(exp).map(_ => ExitCode.Error)
      case Right(config) => BotServer.run[IO](config).map(_ => ExitCode.Success)
    }
  } yield code
}

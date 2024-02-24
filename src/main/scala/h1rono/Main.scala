package h1rono

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  def run = for {
    config <- BotServer.config[IO].value
    n <- BotServer.run[IO](config.toOption.get)
  } yield n
}

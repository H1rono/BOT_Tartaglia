package example

import cats.effect.{IO, IOApp}
import h1rono.BotServer

object Main extends IOApp.Simple {
  val run = BotServer.run[IO]
}

package example

import cats.effect.{IO, IOApp}
import h1rono.BotServer

object Main extends IOApp.Simple {
  val run = for {
    serve <- BotServer.run[IO].toOption.value
  } yield serve.get.use(_ => IO.never)
}

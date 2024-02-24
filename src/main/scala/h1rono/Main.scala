package h1rono

import cats.effect.{IO, IOApp}

object Main extends IOApp.Simple {
  val run = for {
    serve <- BotServer.run[IO].toOption.value
  } yield serve.get.use(_ => IO.never)
}

package h1rono

import org.http4s.Status
import cats.effect.kernel.Async
import org.http4s.Request
import org.typelevel.ci.CIString
import io.circe.Json
import cats.effect._
import org.http4s._
import org.http4s.circe._
import cats.syntax.all._

trait BotHandler[F[_]] {
  def bot(req: Request[F]): F[Status]
}

object BotHandler {
  def impl[F[_]: Async: std.Console]: BotHandler[F] = new BotHandler[F] {
    def bot(req: Request[F]): F[Status] = for {
      payload <- req.as[Json]
      eventType = getHeaderValue(req, CIString("x-traq-bot-event")).getOrElse(
        throw new Exception("x-traq-bot-event not found")
      )
      token = getHeaderValue(req, CIString("x-traq-bot-token")).getOrElse(
        throw new Exception("x-traq-bot-token not found")
      )
      _ <- std.Console[F].println(s"event type is $eventType")
      _ <- std.Console[F].println(payload)
    } yield Status.NoContent

    private def getHeaderValue[F[_]](req: Request[F], key: CIString): Option[String] = for {
      headers <- req.headers.get(key)
      header <- headers.get(0)
    } yield header.value
  }
}

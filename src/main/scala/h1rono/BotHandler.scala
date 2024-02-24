package h1rono

import org.http4s.Status
import cats.effect.kernel.Async
import org.http4s.Request
import org.typelevel.ci.CIString
import io.circe.Json
import cats.effect._
import cats.effect.std.Console
import org.http4s._
import org.http4s.circe._
import cats.syntax.all._
import cats.data.EitherT

trait BotHandler[F[_]] {
  def bot(req: Request[F]): F[Status]
}

object BotHandler {
  final case class Config[F[_]](client: TraqClient[F], verificationToken: String)

  def impl[F[_]: Async: Console](conf: Config[F]): BotHandler[F] = new BotHandler[F] {
    def bot(req: Request[F]): F[Status] = for {
      req <- parseRequest(req).value
      // TODO: Do something with req
    } yield req match {
      case Left(HandleResults.BadInput)        => Status.BadRequest
      case Left(HandleResults.Success)         => Status.NoContent
      case Left(HandleResults.UnexpectedError) => Status.InternalServerError
      case Right(_)                            => Status.NoContent
    }

    private sealed trait HandleResults
    private object HandleResults {
      case object Success extends HandleResults
      case object BadInput extends HandleResults
      case object UnexpectedError extends HandleResults
    }

    private def parseRequest(req: Request[F]): EitherT[F, HandleResults, (String, Json)] = for {
      payload <- EitherT.right(req.as[Json])
      eventType <- checkRequest(req)
      _ <- EitherT.right(Console[F].println(s"event type is $eventType"))
      _ <- EitherT.right(Console[F].println(payload))
    } yield (eventType, payload)

    private def checkRequest(req: Request[F]): EitherT[F, HandleResults, String] = for {
      eventType <- EitherT.fromOption[F](
        getHeaderValue(req, CIString("x-traq-bot-event")),
        HandleResults.BadInput
      )
      token <- EitherT.fromOption[F](
        getHeaderValue(req, CIString("x-traq-bot-token")),
        HandleResults.BadInput
      )
      res <- EitherT.cond(
        token == conf.verificationToken,
        eventType,
        HandleResults.BadInput: HandleResults
      )
    } yield res

    private def getHeaderValue(req: Request[F], key: CIString): Option[String] = for {
      headers <- req.headers.get(key)
      header <- headers.get(0)
    } yield header.value
  }
}

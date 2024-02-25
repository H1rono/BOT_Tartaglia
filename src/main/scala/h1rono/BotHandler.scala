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
import cats.data.{EitherT, OptionT}
import cats.Applicative

trait BotHandler[F[_]] {
  def bot(req: Request[F]): F[Status]
}

object BotHandler {
  final case class Config[F[_]](client: TraqClient[F], verificationToken: String)

  def impl[F[_]: Async: Console](conf: Config[F]): BotHandler[F] = new BotHandler[F] {
    def bot(req: Request[F]): F[Status] = (for {
      req <- parseRequest(req)
      eventType = req._1
      payload = req._2
      res <- handle(eventType, payload)
    } yield res).value.map(_ match {
      case Left(HandleResults.BadInput)        => Status.BadRequest
      case Left(HandleResults.Success)         => Status.NoContent
      case Left(HandleResults.UnexpectedError) => Status.InternalServerError
      case Right(_)                            => Status.NoContent
    })

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

    private def handle(eventType: String, payload: Json): EitherT[F, HandleResults, Unit] =
      eventType match {
        case "JOINED" =>
          EitherT.fromOptionF(
            {
              val channelPathStr = payload.asObject
                .flatMap(_.toMap.get("channel"))
                .flatMap(_.asObject)
                .flatMap(_.toMap.get("path"))
                .flatMap(_.asString)
              channelPathStr match {
                case None => noneF[F, Unit]
                case Some(value) =>
                  for {
                    _ <- Console[F].println(s"joined to channel $value")
                  } yield Some(())
              }
            },
            HandleResults.BadInput
          )
        case "LEFT" =>
          EitherT.fromOptionF(
            {
              val channelPathStr = payload.asObject
                .flatMap(_.toMap.get("channel"))
                .flatMap(_.asObject)
                .flatMap(_.toMap.get("path"))
                .flatMap(_.asString)
              channelPathStr match {
                case None => noneF[F, Unit]
                case Some(path) =>
                  for {
                    _ <- Console[F].println(s"left from channel $path")
                  } yield Some(())
              }
            },
            HandleResults.BadInput
          )
        case "MESSAGE_CREATED" =>
          EitherT.fromOptionF(
            {
              val o = for {
                message <- payload.asObject.flatMap(_.toMap.get("message"))
                user <- message.asObject.flatMap(_.toMap.get("user"))
                username <- user.asObject.flatMap(_.toMap.get("name")).flatMap(_.asString)
                channelId <- message.asObject.flatMap(_.toMap.get("channelId")).flatMap(_.asString)
                plainText <- message.asObject.flatMap(_.toMap.get("plainText")).flatMap(_.asString)
                bot <- user.asObject.flatMap(_.toMap.get("bot")).flatMap(_.asBoolean)
              } yield (username, channelId, plainText, bot)
              o match {
                case None                  => noneF[F, Unit]
                case Some((_, _, _, true)) => noneF[F, Unit]
                case Some((username, channelId, plainText, _)) =>
                  handleMessage(username, channelId, plainText).map(Some(_))
              }
            },
            HandleResults.BadInput
          )
        case _ => EitherT.leftT(HandleResults.Success)
      }

    private def handleMessage(username: String, channelId: String, plainText: String): F[Unit] = {
      val joinRegex = raw"""join""".r
      val leaveRegex = raw"""leave""".r
      val theRegex = raw"""モラがない""".r
      val matchPair = (
        joinRegex.findFirstMatchIn(plainText).isDefined,
        leaveRegex.findFirstMatchIn(plainText).isDefined
      )
      for {
        _ <- Console[F].println(s"message from $username")
        sendTarget = TraqClient.SendTarget.Channel(channelId)
        _ <- conf.client.sendMessage(sendTarget, "ping", false)
        _ <- Console[F].println(s"regex match: $matchPair")
        _ <- matchPair match {
          case (true, false) => conf.client.joinChannel(channelId)
          case (false, true) => conf.client.leaveChannel(channelId)
          case _             => noneF[F, Unit]
        }
        _ <- theRegex.findFirstMatchIn(plainText) match {
          case None => noneF[F, Unit]
          case Some(_) => {
            val target = TraqClient.SendTarget.Channel(channelId)
            conf.client.sendMessage(target, "俺が払うよ", false).map(_ => ())
          }
        }
      } yield ()
    }

    private def noneF[F[_]: Applicative, A] = OptionT.fromOption[F](None: Option[A]).value
  }
}

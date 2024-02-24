package h1rono

import cats.effect.kernel.Async
import org.http4s.client.Client
import cats.effect._
import org.http4s._
import org.http4s.circe._
import org.http4s.client._
import org.http4s.headers._
import io.circe.Json

trait TraqClient[F[_]] {
  def joinChannel(channelId: String): F[Unit]
  def leaveChannel(channelId: String): F[Unit]
  def sendMessage(target: TraqClient.SendTarget, content: String, embed: Boolean): F[Json]
}

object TraqClient {
  sealed trait SendTarget
  final object SendTarget {
    case class Channel(id: String) extends SendTarget
    case class Dm(userId: String) extends SendTarget
  }

  def impl[F[_]: Async](
      base: Client[F],
      botId: String,
      botUserId: String,
      accessToken: String
  ): TraqClient[F] = new TraqClient[F] {
    def joinChannel(channelId: String): F[Unit] = {
      val uri = uriOf(s"/bots/$botId/actions/join")
      val req = Request[F]()
        .withUri(uri)
        .withMethod(Method.POST)
        .withHeaders(`application/json`, authorization)
        .withEntity(Json.obj(("channelId", Json.fromString(botId))))
      base.expect[Unit](req)
    }

    def leaveChannel(channelId: String): F[Unit] = {
      val uri = uriOf(s"/bots/$botId/actions/leave")
      val req = Request[F]()
        .withMethod(Method.POST)
        .withUri(uri)
        .withHeaders(`application/json`, authorization)
        .withEntity(Json.obj(("channelId", Json.fromString(botId))))
      base.expect[Unit](req)
    }

    def sendMessage(target: SendTarget, content: String, embed: Boolean): F[Json] = {
      val uriPath = target match {
        case SendTarget.Channel(id) => s"/channels/$id/messages"
        case SendTarget.Dm(userId)  => s"/users/$userId/messages"
      }
      val uri = uriOf(uriPath)
      val body = Json.obj(
        ("content", Json.fromString(content)),
        ("embed", Json.False)
      )
      val req = Request[F]()
        .withMethod(Method.POST)
        .withUri(uri)
        .withHeaders(`application/json`, authorization)
        .withEntity(body)
      base.expect[Json](req)
    }

    private def uriOf(path: String): Uri =
      Uri.fromString(s"https://q.trap.jp/api/v3$path").toOption.get
    private val `application/json` = `Content-Type`(MediaType.application.json)
    private val authorization = Authorization(Credentials.Token(AuthScheme.Bearer, accessToken))
  }
}

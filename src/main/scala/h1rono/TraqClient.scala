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
  sealed trait SendTarget
  final object SendTarget {
    case class Channel(id: String) extends SendTarget
    case class Dm(userId: String) extends SendTarget
  }

  def joinChannel(channelId: String): F[Unit]
  def leaveChannel(channelId: String): F[Unit]
  def sendMessage(target: SendTarget, content: String, embed: Boolean): F[Json]
}

object TraqClient {
  def impl[F[_]: Async](
      base: Resource[F, Client[F]],
      botId: String,
      botUserId: String,
      accessToken: String
  ): TraqClient[F] = new TraqClient[F] {
    def joinChannel(channelId: String): F[Unit] = {
      val uri = Uri.fromString(s"https://q.trap.jp/api/v3/bots/$botId/actions/join").toOption.get
      val req = Request[F]()
        .withUri(uri)
        .withMethod(Method.POST)
        .withHeaders(`application/json`, authorization)
        .withEntity(Json.obj(("channelId", Json.fromString(botId))))
      base.use(client => client.expect[Unit](req))
    }

    def leaveChannel(channelId: String): F[Unit] = {
      val uri = Uri.fromString(s"https://q.trap.jp/api/v3/bots/$botId/actions/leave").toOption.get
      val req = Request[F]()
        .withMethod(Method.POST)
        .withUri(uri)
        .withHeaders(`application/json`, authorization)
        .withEntity(Json.obj(("channelId", Json.fromString(botId))))
      base.use(client => client.expect[Unit](req))
    }

    def sendMessage(target: SendTarget, content: String, embed: Boolean): F[Json] = {
      val uriStr = target match {
        case SendTarget.Channel(id) => s"https://q.trap.jp/api/v3/channels/$id/messages"
        case SendTarget.Dm(userId)  => s"https://q.trap.jp/api/v3/users/$userId/messages"
      }
      val uri = Uri.fromString(uriStr).toOption.get
      val body = Json.obj(
        ("content", Json.fromString(content)),
        ("embed", Json.False)
      )
      val req = Request[F]()
        .withMethod(Method.POST)
        .withUri(uri)
        .withHeaders(`application/json`, authorization)
        .withEntity()
      base.use(client => client.expect[Json](req))
    }

    private val `application/json` = `Content-Type`(MediaType.application.json)
    private val authorization = Authorization(Credentials.Token(AuthScheme.Bearer, accessToken))
  }
}

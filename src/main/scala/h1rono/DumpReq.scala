package h1rono

import org.http4s.Request
import cats.effect.std._
import cats.syntax.all._
import cats.effect.kernel.Async
import org.http4s.Status

trait DumpReq[F[_]] {
  def dump(req: DumpReq.Req[F]): F[Status]
}

object DumpReq {
  final case class Req[F[_]](req: Request[F]) extends AnyVal

  def impl[F[_]: Async: Console]: DumpReq[F] = new DumpReq[F] {
    def dump(req: Req[F]): F[Status] = for {
      _ <- Console[F].println(req.req)
      body <- req.req.as[String]
      _ <- Console[F].println(body)
    } yield Status.NoContent
  }
}

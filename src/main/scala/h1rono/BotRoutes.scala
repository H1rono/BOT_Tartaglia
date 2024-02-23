package h1rono

import cats.effect.Sync
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.Response

object BotRoutes {
  def helloWorldRoutes[F[_]: Sync](H: HelloWorld[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case GET -> Root / "hello" / name =>
      for {
        greeting <- H.hello(HelloWorld.Name(name))
        resp <- Ok(greeting)
      } yield resp
    }
  }

  def dumpRequestRoutes[F[_]: Sync](D: DumpReq[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case req @ _ -> Root / "dump" =>
      for {
        status <- D.dump(DumpReq.Req(req))
      } yield Response(status = status)
    }
  }

  def botHandlerRoutes[F[_]: Sync](B: BotHandler[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    HttpRoutes.of[F] { case req @ POST -> Root / "bot" =>
      for {
        status <- B.bot(req)
      } yield Response(status = status)
    }
  }
}

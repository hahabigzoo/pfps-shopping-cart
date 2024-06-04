package shop.grpc

import cats.effect.IO
import io.grpc.Metadata
import org.typelevel.log4cats.slf4j.Slf4jLogger
import shop.protobuf.ping._
import org.typelevel.log4cats.Logger


class PingService extends PingServiceFs2Grpc[IO, Metadata] {
  implicit val logger = Slf4jLogger.getLogger[IO]

  override def ping(request: fs2.Stream[IO, PingRequest], ctx: Metadata): fs2.Stream[IO, PingReply] = {
    Logger[IO].info("Call = PingService.ping")
    request.map(pingReq => PingReply("Hello " + pingReq.name))
  }
}

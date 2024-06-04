package shop

import shop.config.Config
import shop.modules._
import shop.resources._
import cats.effect._
import cats.effect.std.Supervisor
import cats.implicits.catsSyntaxTuple2Parallel
import dev.profunktor.redis4cats.log4cats._
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{Server, ServerServiceDefinition}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import shop.grpc.PingService
import shop.protobuf.ping.PingServiceFs2Grpc
import eu.timepit.refined.auto._
import org.typelevel.log4cats.Logger

object Main extends IOApp.Simple {

  implicit val logger = Slf4jLogger.getLogger[IO]

  private val pingService: Resource[IO, ServerServiceDefinition] =
    PingServiceFs2Grpc.bindServiceResource[IO](new PingService)

  private def runServer(service: ServerServiceDefinition): Resource[IO, Server] =
    Resource.make {
      IO {
        val server = NettyServerBuilder
          .forPort(9999)
          .addService(service)
          .addService(ProtoReflectionService.newInstance())
          .build()
        logger.info("gRPC server started on port 9999")
        server.start()
        logger.info("gRPC server started on port 9999")
        server
      }
    } { server =>
      IO {
        server.shutdown()
        logger.info("gRPC server shut down")
      }.void
    }

  override def run: IO[Unit] = {
    Config
      .load[IO]
      .flatMap { cfg =>
        logger.info(s"Loaded config $cfg") >>
          Supervisor[IO].use { implicit sp =>
            AppResources
              .make[IO](cfg)
              .evalMap { res =>
                Security.make[IO](cfg, res.postgres, res.redis).map { security =>
                  val clients  = HttpClients.make[IO](cfg.paymentConfig, res.client)
                  val services = Services.make[IO](res.redis, res.postgres, cfg.cartExpiration)
                  val programs = Programs.make[IO](cfg.checkoutConfig, services, clients)
                  val api      = HttpApi.make[IO](services, programs, security)
                  cfg.httpServerConfig -> api.httpApp
                }
              }
              .flatMap {
                case (cfg, httpApp) => {
                  val httpServer = MkHttpServer[IO].newEmber(cfg, httpApp)
                  val grpcServer = pingService
                    .flatMap(x => runServer(x))
                  (grpcServer, httpServer).parTupled
                }
              }.useForever
          }
      }

  }

}

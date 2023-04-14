package com.vportnov.locations.svc

import cats.effect.{ Sync, Async, ExitCode }
import cats.syntax.all._

import io.grpc.ServerServiceDefinition
import io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all._

import org.slf4j.LoggerFactory

import com.vportnov.locations.svc.Config.Settings
import com.vportnov.locations.grpc.LocationServiceFs2Grpc
import com.vportnov.locations.svc.GrpcService


final class GrpcServer[F[_]: Async](cfg: Settings):
  def run: F[ExitCode] = server

  private def netty(service: ServerServiceDefinition): F[ExitCode] = NettyServerBuilder
    .forPort(cfg.grpc.port.value)
    .addService(service)
    .resource[F]
    .evalMap(server => Sync[F].delay {
      val grpcServer = server.start()
      logger.info(s"grpc server started on ${grpcServer.getListenSockets()}")
      grpcServer
    })
    .use(_ => Async[F].never.as(ExitCode.Success))

  private val logger = LoggerFactory.getLogger(classOf[GrpcServer[F]])
  private val db = new DbStorage(cfg.db)
  private val server = LocationServiceFs2Grpc
    .bindServiceResource(new GrpcService(db))
    .use(netty)
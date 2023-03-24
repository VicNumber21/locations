package com.vportnov.locations.svc

import cats.effect.{ Sync, Async, ExitCode }
import cats.syntax.all._

import io.grpc.ServerServiceDefinition
import io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all._

import com.vportnov.locations.svc.Config.Settings
import com.vportnov.locations.grpc.LocationServiceFs2Grpc
import com.vportnov.locations.svc.GrpcService


final class GrpcServer[F[_]: Async](cfg: Settings):
  def run: F[ExitCode] = server

  private val db = new DbStorage(cfg.db)

  private def netty(service: ServerServiceDefinition): F[ExitCode] = NettyServerBuilder
    .forPort(cfg.grpc.port.value)
    .addService(service)
    .resource[F]
    .evalMap(server => Sync[F].delay {server.start() })
    .use(_ => Async[F].never.as(ExitCode.Success))

  val server = LocationServiceFs2Grpc
    .bindServiceResource(new GrpcService(db))
    .use(netty)
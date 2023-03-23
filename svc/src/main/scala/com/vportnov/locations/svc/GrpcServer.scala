package com.vportnov.locations.svc

import cats.effect.{ Sync, Async, ExitCode }
import cats.syntax.all._

import doobie.Transactor

import io.grpc.ServerServiceDefinition
import io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all._

import com.vportnov.locations.svc.Config.Settings
import com.vportnov.locations.grpc.LocationServiceFs2Grpc
import com.vportnov.locations.svc.GrpcService

final class GrpcServer[F[_]: Async](settings: Settings):
  def run = server

  private val tx = Transactor.fromDriverManager[F](settings.db.driver, settings.db.url, settings.db.user, settings.db.password)
  private val db = new DbStorage(tx)

  private def netty(service: ServerServiceDefinition): F[ExitCode] = NettyServerBuilder
    .forPort(settings.grpc.port.value)
    .addService(service)
    .resource[F]
    .evalMap(server => Sync[F].delay {server.start() })
    .use(_ => Async[F].never.as(ExitCode.Success))

  val server = LocationServiceFs2Grpc
    .bindServiceResource(new GrpcService(db))
    .use(netty)
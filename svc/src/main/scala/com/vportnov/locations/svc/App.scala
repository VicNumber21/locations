package com.vportnov.locations.svc

import cats.effect._
import com.comcast.ip4s._

import com.vportnov.locations.svc.Service

import io.grpc.netty.NettyServerBuilder
import io.grpc._
import fs2.grpc.syntax.all._


object App extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    Service.use(server)

  private def server(service: ServerServiceDefinition): IO[ExitCode] = NettyServerBuilder
    .forPort(9090)
    .addService(service)
    .resource[IO]
    .evalMap(server => IO(server.start()))
    .use(_ => IO.never.as(ExitCode.Success))
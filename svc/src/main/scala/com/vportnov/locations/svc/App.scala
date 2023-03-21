package com.vportnov.locations.svc

import cats.effect._

import com.vportnov.locations.svc.GrpcServer

import io.grpc.netty.NettyServerBuilder
import io.grpc._
import fs2.grpc.syntax.all._


object App extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    GrpcServer.run(nettyServer)

  private def nettyServer(service: ServerServiceDefinition): IO[ExitCode] = NettyServerBuilder
    .forPort(9090)
    .addService(service)
    .resource[IO]
    .evalMap(server => IO(server.start()))
    .use(_ => IO.never.as(ExitCode.Success))
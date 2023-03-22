package com.vportnov.locations.svc

import cats.effect._
import doobie._
import io.grpc._

import com.vportnov.locations.grpc.LocationServiceFs2Grpc
import com.vportnov.locations.svc.GrpcService

object GrpcServer:
  def run[F[_]: Async, T](server: ServerServiceDefinition => F[T]) =
    val tx = Transactor.fromDriverManager[F]("org.postgresql.Driver", "jdbc:postgresql://db:5432/locations", "locator", "locator")
    val db = new StorageDb(tx)

    LocationServiceFs2Grpc
      .bindServiceResource(new GrpcService(db))
      .use(server)
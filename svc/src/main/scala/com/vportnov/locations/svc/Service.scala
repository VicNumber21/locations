package com.vportnov.locations.svc

import cats.effect._

import doobie._

import io.grpc._

import fs2.Stream

import com.vportnov.locations.grpc.LocationsFs2Grpc
import com.vportnov.locations.grpc
import com.vportnov.locations.grpc.bindings._
import com.vportnov.locations.model

object Service:
  def use[F[_]: Async, T](server: ServerServiceDefinition => F[T]) =
    LocationsFs2Grpc
    .bindServiceResource(new ServiceImpl())
    .use(server)


final class ServiceImpl[F[_]: Async] extends LocationsFs2Grpc[F, Metadata]:
  override def locationStats(period: grpc.Period, ctx: Metadata): Stream[F, grpc.LocationStats] =
    db.locationStats(period.toModel).map(_.toMessage)

  private val tx = Transactor.fromDriverManager[F]("org.postgresql.Driver", "jdbc:postgresql://db:5432/locations", "locator", "locator")
  private val db = new StorageDb(tx)



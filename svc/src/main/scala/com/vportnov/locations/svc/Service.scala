package com.vportnov.locations.svc

import cats.effect._
import cats.syntax.all._

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
  override def locationStats(request: grpc.Period, ctx: Metadata): Stream[F, grpc.LocationStats] =
    import java.time.LocalDateTime
    val stats = model.Location.Stats(LocalDateTime.now.toLocalDate, 15)
    Stream(stats.toMessage)
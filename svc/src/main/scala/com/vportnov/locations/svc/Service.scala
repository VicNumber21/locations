package com.vportnov.locations.svc

import cats.effect._
import cats.syntax.all._

import io.grpc._

import fs2.Stream

import com.vportnov.locations.grpc.LocationsFs2Grpc
import com.vportnov.locations.grpc._

object Service:
  def use[F[_]: Async, T](server: ServerServiceDefinition => F[T]) =
    LocationsFs2Grpc
    .bindServiceResource(new ServiceImpl())
    .use(server)


final class ServiceImpl[F[_]: Async] extends LocationsFs2Grpc[F, Metadata]:
  override def locationStats(request: com.vportnov.locations.grpc.Period, ctx: Metadata): Stream[F, LocationStats] =
    import com.google.protobuf.timestamp.Timestamp
    import java.time._
    val now = LocalDateTime.now().atZone(ZoneOffset.UTC)
    Stream(LocationStats(Some(Timestamp(now.toEpochSecond, now.getNano)), 12))
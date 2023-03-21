package com.vportnov.locations.svc

import io.grpc._
import fs2.Stream

import com.vportnov.locations.grpc.LocationsFs2Grpc
import com.vportnov.locations.grpc
import com.vportnov.locations.grpc.bindings._
import com.vportnov.locations.model


final class GrpcService[F[_]](storage: model.Storage[F]) extends LocationsFs2Grpc[F, Metadata]:
  override def locationStats(period: grpc.Period, ctx: Metadata): Stream[F, grpc.LocationStats] =
    storage.locationStats(period.toModel).map(_.toMessage)
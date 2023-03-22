package com.vportnov.locations.svc

import io.grpc._
import fs2.Stream

import com.vportnov.locations.grpc.LocationServiceFs2Grpc
import com.vportnov.locations.grpc
import com.vportnov.locations.grpc.bindings._
import com.vportnov.locations.model


final class GrpcService[F[_]](storage: model.Storage[F]) extends LocationServiceFs2Grpc[F, Metadata]:
  def createLocations(locations: grpc.Locations, ctx: Metadata): Stream[F, grpc.Location] =
    storage.createLocations(locations.toLocationsWithOptionalCreatedField).map(_.toMessage)

  def getLocations(request: grpc.GetRequest, ctx: Metadata): Stream[F, grpc.Location] =
    storage.getLocations(request.getPeriod.toModel, request.ids.toList).map(_.toMessage)

  override def locationStats(period: grpc.Period, ctx: Metadata): Stream[F, grpc.LocationStats] =
    storage.locationStats(period.toModel).map(_.toMessage)
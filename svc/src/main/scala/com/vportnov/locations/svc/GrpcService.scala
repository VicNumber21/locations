package com.vportnov.locations.svc

import cats.effect.kernel.Async
import cats.implicits._

import io.grpc._
import fs2.Stream

import com.vportnov.locations.grpc
import com.vportnov.locations.grpc.bindings._
import com.vportnov.locations.model
import com.vportnov.locations.utils.LoggingIO


final class GrpcService[F[_]: Async](storage: model.Storage[F]) extends grpc.LocationServiceFs2Grpc[F, Metadata] with LoggingIO[F]:
  def createLocations(locations: grpc.Locations, ctx: Metadata): Stream[F, grpc.LocationReply] =
    storage.createLocations(locations.toLocationsWithOptionalCreatedField)
      .logError
      .packLocation
      .logWhenDone

  def getLocations(query: grpc.Query, ctx: Metadata): Stream[F, grpc.LocationReply] =
    storage.getLocations(query.getPeriod.toModel, query.ids.toModel)
      .logError
      .packLocation
      .logWhenDone

  def updateLocations(locations: grpc.Locations, ctx: Metadata): Stream[F, grpc.LocationReply] =
    storage.updateLocations(locations.toLocationsWithoutCreatedField)
      .logError
      .packLocation
      .logWhenDone

  override def deleteLocations(ids: grpc.Ids, ctx: Metadata): F[grpc.CountReply] =
    storage.deleteLocations(ids.toModel)
      .logError
      .packCount
      .logError

  override def locationStats(period: grpc.Period, ctx: Metadata): Stream[F, grpc.LocationStatsReply] =
    storage.locationStats(period.toModel)
      .logError
      .packLocationStats
      .logWhenDone

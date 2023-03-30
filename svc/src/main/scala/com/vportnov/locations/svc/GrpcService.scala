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
  def createLocations(locations: grpc.Locations, ctx: Metadata): Stream[F, grpc.Location] =
    storage.createLocations(locations.toLocationsWithOptionalCreatedField)
      .map(_.toMessage)
      .logWhenDone

  def getLocations(query: grpc.Query, ctx: Metadata): Stream[F, grpc.Location] =
    storage.getLocations(query.getPeriod.toModel, query.ids.toList)
      .map(_.toMessage)
      .logWhenDone

  def updateLocations(locations: grpc.Locations, ctx: Metadata): Stream[F, grpc.Location] =
    storage.updateLocations(locations.toLocationsWithoutCreatedField)
      .map(_.toMessage)
      .logWhenDone

  override def deleteLocations(ids: grpc.Ids, ctx: Metadata): Stream[F, grpc.Count] =
    val count = storage.deleteLocations(ids.ids.toList)
      .logWhenDone
    Stream.eval(count).map(grpc.Count(_))

  override def locationStats(period: grpc.Period, ctx: Metadata): Stream[F, grpc.LocationStats] =
    storage.locationStats(period.toModel)
      .map(_.toMessage)
      .logWhenDone

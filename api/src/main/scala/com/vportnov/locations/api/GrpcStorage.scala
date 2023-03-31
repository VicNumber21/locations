package com.vportnov.locations.api

import cats.effect.{ Async, Sync, Resource }
import cats.syntax.all._
import org.typelevel.log4cats.syntax._

import io.grpc.netty.NettyChannelBuilder
import fs2.grpc.syntax.all._
import fs2.Stream
import io.grpc._

import com.vportnov.locations.api.Config.Address
import com.vportnov.locations.model
import com.vportnov.locations.grpc
import com.vportnov.locations.grpc.bindings._


final class GrpcStorage[F[_]: Async](grpcAddress: Address) extends model.StorageExt[F]:
  override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[F] =
    for
      grpcApi <- grpcApiStream
      location <- grpcApi.createLocations(locations.toMessage, new Metadata)
    yield location.unpackLocation

  override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[F] =
    for
      grpcApi <- grpcApiStream
      query = grpc.Query().withPeriod(period.toMessage).withIds(ids.toMessage)
      location <- grpcApi.getLocations(query, new Metadata)
    yield location.unpackLocation

  override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[F] =
    for
      grpcApi <- grpcApiStream
      location <- grpcApi.updateLocations(locations.toMessage, new Metadata)
    yield location.unpackLocation

  override def deleteLocations(ids: model.Location.Ids): F[Int] =
    grpcClient.use { grpcApi => grpcApi.deleteLocations(grpc.Ids(ids), new Metadata).unpackCount }

  override def locationStats(period: model.Period): LocationStatsStream[F] =
    for
      grpcApi <- grpcApiStream
      stats <- grpcApi.locationStats(period.toMessage, new Metadata)
    yield stats.unpackLocationStats

  private val grpcClient: Resource[F, grpc.LocationServiceFs2Grpc[F, Metadata]] =
    NettyChannelBuilder
      .forAddress(grpcAddress.host.toString, grpcAddress.port.value)
      .usePlaintext()
      .resource[F]
      .flatMap(grpc.LocationServiceFs2Grpc.stubResource(_))
    
  private def grpcApiStream: Stream[F, grpc.LocationServiceFs2Grpc[F, Metadata]] = Stream.resource(grpcClient)

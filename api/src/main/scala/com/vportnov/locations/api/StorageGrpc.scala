package com.vportnov.locations.api

import cats.effect._
// TODO check that it should be here
import io.grpc.netty.NettyChannelBuilder
import fs2.grpc.syntax.all._
import fs2.Stream
import io.grpc._

import com.vportnov.locations.model
import com.vportnov.locations.grpc.LocationServiceFs2Grpc
import com.vportnov.locations.grpc
import com.vportnov.locations.grpc.bindings._

// TODO remove StorageDb during rework to eal gRpc
final class StorageGrpc[F[_]: Async](db: StorageDb[F]) extends model.StorageExt[F]:
  override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[F] =
    for {
      grpcApi <- Stream.resource(grpcClient)
      location <- grpcApi.createLocations(locations.toMessage, new Metadata)
    } yield location.toLocationWithCreatedField

  override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[F] =
    for {
      grpcApi <- Stream.resource(grpcClient)
      query = grpc.Query().withPeriod(period.toMessage).withIds(ids)
      location <- grpcApi.getLocations(query, new Metadata)
    } yield location.toLocationWithCreatedField

  override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[F] =
    db.updateLocations(locations)

  override def deleteLocations(ids: model.Location.Ids): F[Either[Throwable, Int]] =
    db.deleteLocations(ids)

  override def locationStats(period: model.Period): LocationStatsStream[F] =
    for {
      grpcApi <- Stream.resource(grpcClient)
      stats <- grpcApi.locationStats(new grpc.Period(), new Metadata)
    } yield stats.toModel

  val managedChannelResource: Resource[F, ManagedChannel] =
    NettyChannelBuilder
      .forAddress("svc", 9090)
      .usePlaintext()
      .resource[F]
  
  val grpcClient = managedChannelResource
    .flatMap(LocationServiceFs2Grpc.stubResource(_))
  

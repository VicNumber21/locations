package com.vportnov.locations.api

import cats.effect._
// TODO check that it should be here
import io.grpc.netty.NettyChannelBuilder
import fs2.grpc.syntax.all._
import fs2.Stream
import io.grpc._

import com.vportnov.locations.api.types.lib._
import com.vportnov.locations.grpc.LocationsFs2Grpc
import com.vportnov.locations.{grpc => locGrpc}

// TODO remove StorageDb during rework to eal gRpc
final class StorageGrpc[F[_]: Async](db: StorageDb[F]) extends StorageExt[F]:
  override def createLocations(locations: List[Location.WithOptionalCreatedField]): LocationStream[F] =
    db.createLocations(locations)

  override def getLocations(period: Period, ids: Location.Ids): LocationStream[F] =
    db.getLocations(period, ids)

  override def updateLocations(locations: List[Location.WithoutCreatedField]): LocationStream[F] =
    db.updateLocations(locations)

  override def deleteLocations(ids: Location.Ids): F[Either[Throwable, Int]] =
    db.deleteLocations(ids)

  override def locationStats(period: Period): LocationStatsStream[F] =
    // db.locationStats(period)
    import java.time.ZoneOffset
    for {
      grpcApi <- Stream.resource(grpc)
      stats <- grpcApi.locationStats(new locGrpc.Period(), new Metadata())
      date = java.time.LocalDateTime.ofEpochSecond(stats.date.get.seconds, stats.date.get.nanos, ZoneOffset.UTC).toLocalDate()
    } yield Location.Stats(date, stats.count)

  val managedChannelResource: Resource[F, ManagedChannel] =
    NettyChannelBuilder
      .forAddress("svc", 9090)
      .usePlaintext()
      .resource[F]
  
  val grpc = managedChannelResource
    .flatMap(LocationsFs2Grpc.stubResource(_))
  

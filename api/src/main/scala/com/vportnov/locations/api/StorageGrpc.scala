package com.vportnov.locations.api

import cats.effect.{ Async, Resource }
import io.grpc.netty.NettyChannelBuilder
import fs2.grpc.syntax.all._
import fs2.Stream
import io.grpc._

import com.vportnov.locations.model
import com.vportnov.locations.model.StreamExtOps._ // TODO required if deleteLocations is kept using stream as transport
import com.vportnov.locations.grpc.LocationServiceFs2Grpc
import com.vportnov.locations.grpc
import com.vportnov.locations.grpc.bindings._


final class StorageGrpc[F[_]: Async] extends model.StorageExt[F]:
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
    for {
      grpcApi <- Stream.resource(grpcClient)
      location <- grpcApi.updateLocations(locations.toMessage, new Metadata)
    } yield location.toLocationWithCreatedField

  override def deleteLocations(ids: model.Location.Ids): F[Int] =
    val countStream: Stream[F, Int] = for {
      grpcApi <- Stream.resource(grpcClient)
      count <- grpcApi.deleteLocations(grpc.Ids(ids), new Metadata)
    } yield count.count

    countStream.firstEntry


  override def locationStats(period: model.Period): LocationStatsStream[F] =
    for {
      grpcApi <- Stream.resource(grpcClient)
      stats <- grpcApi.locationStats(new grpc.Period(), new Metadata)
    } yield stats.toModel

  private val managedChannelResource: Resource[F, ManagedChannel] =
    NettyChannelBuilder
      .forAddress("svc", 9090)
      .usePlaintext()
      .resource[F]
  
  private val grpcClient = managedChannelResource
    .flatMap(LocationServiceFs2Grpc.stubResource(_))
  

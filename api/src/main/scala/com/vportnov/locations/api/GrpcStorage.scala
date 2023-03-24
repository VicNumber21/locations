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
import com.vportnov.locations.utils.fs2stream.syntax._
import com.vportnov.locations.grpc.LocationServiceFs2Grpc
import com.vportnov.locations.grpc
import com.vportnov.locations.grpc.bindings._
import com.vportnov.locations.utils.LoggingIO


final class GrpcStorage[F[_]: Async](grpcAddress: Address) extends model.StorageExt[F] with LoggingIO[F]:
  override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[F] =
    (for
      grpcApi <- grpcApiStream
      location <- grpcApi.createLocations(locations.toMessage, new Metadata)
    yield location.toLocationWithCreatedField)
      .onFinalize(info"${currentMethodName} stream is done")
      .onError(logStreamError(s"Exception on request ${currentMethodName}"))

  override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[F] =
    (for
      grpcApi <- grpcApiStream
      query = grpc.Query().withPeriod(period.toMessage).withIds(ids)
      location <- grpcApi.getLocations(query, new Metadata)
    yield location.toLocationWithCreatedField)
      .onFinalize(info"${currentMethodName} stream is done")
      .onError(logStreamError(s"Exception on request ${currentMethodName}"))

  override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[F] =
    (for
      grpcApi <- grpcApiStream
      location <- grpcApi.updateLocations(locations.toMessage, new Metadata)
    yield location.toLocationWithCreatedField)
      .onFinalize(info"${currentMethodName} stream is done")
      .onError(logStreamError(s"Exception on request ${currentMethodName}"))

  override def deleteLocations(ids: model.Location.Ids): F[Int] =
    val countStream: Stream[F, Int] =
      (for
        grpcApi <- grpcApiStream
        count <- grpcApi.deleteLocations(grpc.Ids(ids), new Metadata)
      yield count.count)
        .onFinalize(info"${currentMethodName} stream is done")
        .onError(logStreamError(s"Exception on request ${currentMethodName}"))

    for
      count <- countStream.firstEntry
      _ <- if count < 0 then error"Strange but count is less than 0 (count = ${count})" else ().pure[F]
    yield count
  end deleteLocations

  override def locationStats(period: model.Period): LocationStatsStream[F] =
    (for
      grpcApi <- grpcApiStream
      stats <- grpcApi.locationStats(new grpc.Period(), new Metadata)
    yield stats.toModel)
      .onFinalize(info"${currentMethodName} stream is done")
      .onError(logStreamError(s"Exception on request ${currentMethodName}"))

  private val grpcClient: Resource[F, LocationServiceFs2Grpc[F, Metadata]] =
    NettyChannelBuilder
      .forAddress(grpcAddress.host.toString, grpcAddress.port.value)
      .usePlaintext()
      .resource[F]
      .flatMap(LocationServiceFs2Grpc.stubResource(_))
    
  private def grpcApiStream: Stream[F, LocationServiceFs2Grpc[F, Metadata]] = Stream.resource(grpcClient)
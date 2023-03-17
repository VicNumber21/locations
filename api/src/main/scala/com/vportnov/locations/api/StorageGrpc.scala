package com.vportnov.locations.api

import cats.effect.Sync

import com.vportnov.locations.api.types.lib._

// TODO remove StorageDb during rework to eal gRpc
final class StorageGrpc[F[_]: Sync](db: StorageDb[F]) extends StorageExt[F]:
  override def createLocations(locations: List[Location.WithOptionalCreatedField]): LocationStream[F] =
    db.createLocations(locations)

  override def getLocations(period: Period, ids: Location.Ids): LocationStream[F] =
    db.getLocations(period, ids)

  override def updateLocations(locations: List[Location.WithoutCreatedField]): LocationStream[F] =
    db.updateLocations(locations)

  override def deleteLocations(ids: Location.Ids): F[Either[Throwable, Int]] =
    db.deleteLocations(ids)

  override def locationStats(period: Period): LocationStatsStream[F] =
    db.locationStats(period)
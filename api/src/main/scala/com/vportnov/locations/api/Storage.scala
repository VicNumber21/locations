package com.vportnov.locations.api

import fs2.Stream

import com.vportnov.locations.api.types.lib._



trait Storage[F[_]]:
  type LocationStream[F[_]] = Stream[F, Location.WithCreatedField]
  type LocationStatsStream[F[_]] = Stream[F, Location.Stats]

  def createLocations(locations: List[Location.WithOptionalCreatedField]): LocationStream[F]

  def getLocations(period: Period, ids: Location.Ids): LocationStream[F]

  def updateLocations(locations: List[Location.WithoutCreatedField]): LocationStream[F]

  def deleteLocations(ids: Location.Ids): F[Either[Throwable, Int]]
  
  def locationStats(period: Period): LocationStatsStream[F]


// TODO move to another file
import cats.effect.Sync
import fs2.Stream
import cats.implicits._

trait StorageExt[F[_]: Sync] extends Storage[F]:
  import FirstEntryStreamOps._

  def createLocation(location: Location.WithOptionalCreatedField): F[Location.WithCreatedField] = 
    createLocations(List(location)).firstEntry

  def getLocation(id: Location.Id): F[Location.WithCreatedField] = 
    getLocations(Period(None, None), List(id)).firstEntry

  def updateLocation(location: Location.WithoutCreatedField): F[Location.WithCreatedField] = 
    updateLocations(List(location)).firstEntry

  def deleteLocation(id: Location.Id): F[Either[Throwable, Int]] =
    deleteLocations(List(id))
  

object FirstEntryStreamOps:
  extension [F[_]: Sync, O] (stream: Stream[F, O])
    def firstEntry: F[O] =
      for {
        list <- stream.take(1).compile.toList
      } yield list.head
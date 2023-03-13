package com.vportnov.locations.api

import fs2.Stream
import cats.effect.Async // TODO probably Sync should be here; check after rework to gRPC
import cats.implicits._

import com.vportnov.locations.api.types.lib._

final class StorageGrpc[F[_]: Async] extends Storage[F]:
  import com.vportnov.locations.api.StorageGrpc._

  // TODO rework to gRPC client
  import doobie._
  val tx = Transactor.fromDriverManager[F]("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/locations", "locator", "locator")
  val db = new StorageDb(tx)

  override def createLocations(locations: List[Location.WithOptionalCreatedField]): LocationStream[F] =
    db.createLocations(locations)

  def createLocation(location: Location.WithOptionalCreatedField): F[Location.WithCreatedField] = 
    createLocations(List(location)).firstEntry

  override def getLocations(period: Period, ids: Location.Ids): LocationStream[F] =
    db.getLocations(period, ids)

  def getLocation(id: Location.Id): F[Location.WithCreatedField] = 
    getLocations(Period(None, None), List(id)).firstEntry

  override def updateLocations(locations: List[Location.WithoutCreatedField]): LocationStream[F] =
    db.updateLocations(locations)

  def updateLocation(location: Location.WithoutCreatedField): F[Location.WithCreatedField] = 
    updateLocations(List(location)).firstEntry

  override def deleteLocations(ids: Location.Ids): F[Int] =
    db.deleteLocations(ids)

  def deleteLocation(id: Location.Id): F[Int] = 
    deleteLocations(List(id))
  
  override def locationStats(period: Period): LocationStatsStream[F] =
    db.locationStats(period)


object StorageGrpc:
  extension [F[_]: Async, O] (stream: Stream[F, O])
    def firstEntry: F[O] =
      for {
        list <- stream.take(1).compile.toList
      } yield list.head
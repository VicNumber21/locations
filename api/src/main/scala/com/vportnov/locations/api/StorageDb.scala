// TODO move to svc
package com.vportnov.locations.api

import cats.effect.Sync
import cats.data.NonEmptyList
import cats.implicits._

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import com.vportnov.locations.api.types.lib._


final class StorageDb[F[_]: Sync](tx: Transactor[F]) extends Storage[F]:
  import StorageDb._
  override def createLocations(locations: List[Location.WithOptionalCreatedField]): LocationStream[F] =
    sql.insert.locations(locations)
      .withGeneratedKeys[Location.WithCreatedField]("location_id", "location_longitude", "location_latitude", "location_created")
      .transact(tx)

  override def getLocations(period: Period, ids: Location.Ids): LocationStream[F] =
    sql.select.locations(period, ids) match
      case Some(query) =>
        query.stream.transact(tx)
      case None =>
        fs2.Stream.raiseError(new RuntimeException) // TODO add better error

  override def updateLocations(locations: List[Location.WithoutCreatedField]): LocationStream[F] =
    ???

  override def deleteLocations(ids: Location.Ids): CountStream[F] =
    ???

  override def locationStats(period: Period): LocationStatsStream[F] =
    ???
  
  

object StorageDb:
  //TODO test sql
  object sql:
    object select:
      val base = 
          fr"SELECT location_id, location_longitude, location_latitude, location_created" ++
          fr"FROM locations"

      def locations(period: Period, ids: Location.Ids): Option[Query0[Location.WithCreatedField]] =
        val query = if !period.isEmpty && !ids.isEmpty
          then None
        else
          Some(select.base ++ Fragments.whereAndOpt((byIds(ids) :: byPeriod(period)):_*))
        
        query.map(q => q.query[Location.WithCreatedField])

      def byPeriod(period: Period): List[Option[Fragment]] = 
        val from = period.from.map(from => fr"location_created >= CAST($from AS DATE)")
        val to = period.to.map(to => fr"location_created < (CAST($to AS DATE) + CAST('1 day' AS INTERVAL))")
        List(from, to)

      def byIds(ids: Location.Ids): Option[Fragment] = 
        ids.toNel.map(nelIds => Fragments.in(fr"location_id", nelIds))

    object insert:
      def value(location: Location.WithOptionalCreatedField): Fragment = location match
        case Location.WithOptionalCreatedField(id, longitude, latitude, Some(created)) =>
          sql"($id, $longitude, $latitude, $created)"
        case Location.WithOptionalCreatedField(id, longitude, latitude, None) =>
          sql"($id, $longitude, $latitude, CURRENT_TIMESTAMP)"
      
      def values(locations: List[Location.WithOptionalCreatedField]): Fragment =
        locations.map(value).foldSmash(fr"VALUES", fr",", fr"")

      def locations(locations: List[Location.WithOptionalCreatedField]): Update0 =
        (
          fr"INSERT INTO locations (location_id, location_longitude, location_latitude, location_created)" ++
          values(locations) ++
          fr"ON CONFLICT (location_id) DO NOTHING"
        ) .update

    object delete:
      val all = sql"DELETE FROM locations"
        .update



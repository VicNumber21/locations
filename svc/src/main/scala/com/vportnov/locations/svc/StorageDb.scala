package com.vportnov.locations.svc

import cats.effect.Sync
import cats.data.NonEmptyList
import cats.implicits._

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import com.vportnov.locations.model._


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
        fs2.Stream.raiseError(new IllegalArgumentException("Should not request both filters by ids and by dates"))

  override def updateLocations(locations: List[Location.WithoutCreatedField]): LocationStream[F] =
    sql.update.locations(locations)
      .withGeneratedKeys[Location.WithCreatedField]("location_id", "location_longitude", "location_latitude", "location_created")
      .transact(tx)

  def deleteLocations(ids: Location.Ids): F[Int] =
    if ids.isEmpty
      then Sync[F].raiseError(new IllegalArgumentException("Ids list must not be empty"))
      else
          sql.delete.locations(ids)
            .run
            .transact(tx)

  override def locationStats(period: Period): LocationStatsStream[F] =
    sql.select.stats(period).stream.transact(tx)
  

object StorageDb:
  //TODO test sql
  object sql:
    object select:
      def locations(period: Period, ids: Location.Ids): Option[Query0[Location.WithCreatedField]] =
        val query = if !period.isEmpty && !ids.isEmpty
          then None
        else
          Some(
            fr"SELECT location_id, location_longitude, location_latitude, location_created" ++
            fr"FROM locations" ++
            Fragments.whereAndOpt((byIds(ids) :: byPeriod(period)):_*)
          )
        
        query.map(q => q.query[Location.WithCreatedField])
      
      def stats(period: Period): Query0[Location.Stats] =
        (
          fr"SELECT" ++
            fr"CAST(location_created AS DATE) AS created_date," ++
            fr"COUNT(location_created) AS created_locations" ++
          fr"FROM locations" ++
          Fragments.whereAndOpt(byPeriod(period):_*) ++
          fr"GROUP BY created_date" ++
          fr"ORDER BY created_date"
        )
          .query[Location.Stats]

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
        )
          .update

    object update:
      def value(location: Location.WithoutCreatedField): Fragment =
        val Location.WithoutCreatedField(id, longitude, latitude) = location
        sql"($id, $longitude, $latitude)"
      
      def values(locations: List[Location.WithoutCreatedField]): Fragment =
        locations.map(value).foldSmash(fr"VALUES", fr",", fr"")

      def locations(locations: List[Location.WithoutCreatedField]): Update0 =
        (
          fr"UPDATE locations AS l" ++
          fr"SET location_longitude = location.longitude, location_latitude = location.latitude" ++
          fr"FROM (" ++
          update.values(locations) ++
          fr") AS location (id, longitude, latitude)" ++
          fr"WHERE l.location_id = location.id"
        )
          .update

    object delete:
      def locations(ids: Location.Ids): Update0 =
        (
          fr"DELETE FROM locations" ++
          Fragments.whereAndOpt(select.byIds(ids))
        )
          .update



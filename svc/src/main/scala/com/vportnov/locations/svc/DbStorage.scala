package com.vportnov.locations.svc

import cats.effect.{ Sync, Async }
import cats.data.NonEmptyList
import cats.implicits._

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import com.vportnov.locations.svc.Config
import com.vportnov.locations.model._
import com.vportnov.locations.utils.ServerError


final class DbStorage[F[_]: Async](db: Config.Database) extends Storage[F]:
  import DbStorage._
  override def createLocations(locations: List[Location.WithOptionalCreatedField]): LocationStream[F] =
    sql.insert.locations(locations)
      .withGeneratedKeys[Location.WithCreatedField]("location_id", "location_longitude", "location_latitude", "location_created")
      .transact(tx)

  override def getLocations(period: Period, ids: Location.Ids): LocationStream[F] =
    sql.select.locations(period, ids) match
      case Some(query) =>
        query.stream.transact(tx)
      case None =>
        fs2.Stream.raiseError(ServerError.IllegalArgument("Should not request both filters by ids and by dates"))

  override def updateLocations(locations: List[Location.WithoutCreatedField]): LocationStream[F] =
    sql.update.locations(locations)
      .withGeneratedKeys[Location.WithCreatedField]("location_id", "location_longitude", "location_latitude", "location_created")
      .transact(tx)

  override def deleteLocations(ids: Location.Ids): F[Int] =
    if ids.isEmpty
      then Sync[F].raiseError(ServerError.IllegalArgument("Ids list must not be empty"))
      else sql.delete.locations(ids) .run .transact(tx)

  override def locationStats(period: Period): LocationStatsStream[F] =
    sql.select.stats(period).stream.transact(tx)

  private val tx = Transactor.fromDriverManager[F](db.driver, db.userUrl, db.user.login, db.user.password)
  

object DbStorage:
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
            fr"CAST(CAST(location_created AS DATE) AS TIMESTAMP) AS created_date," ++
            fr"CAST(COUNT(location_created) AS INTEGER) AS created_locations" ++
          fr"FROM locations" ++
          Fragments.whereAndOpt(byPeriod(period):_*) ++
          fr"GROUP BY created_date" ++
          fr"ORDER BY created_date"
        )
          .query[Location.Stats]

      def byPeriod(period: Period): List[Option[Fragment]] = 
        val from = period.from.map(from => fr"location_created >= CAST(${from.toLocalDate} AS DATE)")
        val to = period.to.map(to => fr"location_created < (CAST(${to.toLocalDate} AS DATE) + CAST('1 day' AS INTERVAL))")
        List(from, to)

      def byIds(ids: Location.Ids): Option[Fragment] = 
        ids.toNel.map(nelIds => Fragments.in(fr"location_id", nelIds))

    object insert:
      def value(location: Location.WithOptionalCreatedField): Fragment = location match
        case Location.WithOptionalCreatedField(id, longitude, latitude, Some(created)) =>
          sql"(CAST($id AS VARCHAR), CAST($longitude AS NUMERIC), CAST($latitude AS NUMERIC), CAST($created AS TIMESTAMP))"
        case Location.WithOptionalCreatedField(id, longitude, latitude, None) =>
          sql"(CAST($id AS VARCHAR), CAST($longitude AS NUMERIC), CAST($latitude AS NUMERIC), CURRENT_TIMESTAMP)"
      
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
        sql"(CAST($id AS VARCHAR), CAST($longitude AS NUMERIC), CAST($latitude AS NUMERIC))"
      
      def values(locations: List[Location.WithoutCreatedField]): Fragment =
        locations.map(value).foldSmash(fr"VALUES", fr",", fr"")

      def locations(locations: List[Location.WithoutCreatedField]): Update0 =
        (
          fr"UPDATE locations AS l" ++
          fr"SET location_longitude = loc.longitude, location_latitude = loc.latitude" ++
          fr"FROM (" ++
          update.values(locations) ++
          fr") AS loc (id, longitude, latitude)" ++
          fr"WHERE l.location_id = loc.id"
        )
          .update

    object delete:
      def locations(ids: Location.Ids): Update0 =
        (
          fr"DELETE FROM locations" ++
          Fragments.whereAndOpt(select.byIds(ids))
        )
          .update
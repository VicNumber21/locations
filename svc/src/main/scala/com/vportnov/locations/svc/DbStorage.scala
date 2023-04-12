package com.vportnov.locations.svc

import cats.effect.{ Sync, Async }
import cats.data.NonEmptyList
import cats.implicits._
import cats.syntax.all._

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._

import com.vportnov.locations.svc.Config
import com.vportnov.locations.model._
import com.vportnov.locations.utils.ServerError
import com.vportnov.locations.utils.fs2stream.syntax._


final class DbStorage[F[_]: Async](db: Config.Database) extends Storage[F]:
  import DbStorage._

  override def createLocations(locations: List[Location.WithOptionalCreatedField]): LocationStream[F] =
    for {
      program <- sql.insert.locations(locations).liftToStream
      result <- program
        .withGeneratedKeys[Location.WithCreatedField]("location_id", "location_longitude", "location_latitude", "location_created")
        .transact(tx)
    } yield result

  override def getLocations(period: Period, ids: Location.Ids): LocationStream[F] =
    for {
      program <- sql.select.locations(period, ids).liftToStream
      result <- program.stream.transact(tx)
    } yield result

  override def updateLocations(locations: List[Location.WithoutCreatedField]): LocationStream[F] =
    for {
      program <- sql.update.locations(locations).liftToStream
      result <- program
        .withGeneratedKeys[Location.WithCreatedField]("location_id", "location_longitude", "location_latitude", "location_created")
        .transact(tx)
    } yield result

  override def deleteLocations(ids: Location.Ids): F[Int] =
    for {
      program <- sql.delete.locations(ids).liftTo[F]
      count <- program.run.transact(tx)
    } yield count

  override def locationStats(period: Period): LocationStatsStream[F] =
    sql.select.stats(period).stream.transact(tx)

  private val tx = Transactor.fromDriverManager[F](db.driver, db.userUrl, db.user.login, db.user.password)
  

object DbStorage:
  object sql:
    object select:
      def locations(period: Period, ids: Location.Ids): Either[Throwable, Query0[Location.WithCreatedField]] =
        (period, ids) match
          case (period, ids) if !period.isEmpty && !ids.isEmpty =>
            ServerError.IllegalArgument("Should not request both filters by ids and by dates").asLeft
          case (period, ids) =>
            sqlScriptLocations(period, ids).query[Location.WithCreatedField].asRight
      
      def stats(period: Period): Query0[Location.Stats] =
        sqlScriptStats(period).query[Location.Stats]

      def byPeriod(period: Period): List[Option[Fragment]] = 
        val from = period.from.map(from => fr"location_created >= CAST(${from.toLocalDate} AS DATE)")
        val to = period.to.map(to => fr"location_created < (CAST(${to.toLocalDate} AS DATE) + CAST('1 day' AS INTERVAL))")
        List(from, to)

      def byIds(ids: Location.Ids): Option[Fragment] = 
        ids.toNel.map(nelIds => Fragments.in(fr"location_id", nelIds))

      private def sqlScriptLocations(period: Period, ids: Location.Ids): Fragment =
        fr"SELECT location_id, location_longitude, location_latitude, location_created" ++
        fr"FROM locations" ++
        Fragments.whereAndOpt((byIds(ids) :: byPeriod(period)):_*)

      private def sqlScriptStats(period: Period): Fragment =
        fr"SELECT" ++
          fr"CAST(CAST(location_created AS DATE) AS TIMESTAMP) AS created_date," ++
          fr"CAST(COUNT(location_created) AS INTEGER) AS created_locations" ++
        fr"FROM locations" ++
        Fragments.whereAndOpt(byPeriod(period):_*) ++
        fr"GROUP BY created_date" ++
        fr"ORDER BY created_date"


    object insert:
      def locations(locations: List[Location.WithOptionalCreatedField]): Either[Throwable ,Update0] =
        locations match
          case List() => ServerError.IllegalArgument("Location list must not be empty").asLeft
          case _ => sqlScript(locations).update.asRight

      private def sqlScript(locations: List[Location.WithOptionalCreatedField]): Fragment =
        fr"INSERT INTO locations (location_id, location_longitude, location_latitude, location_created)" ++
        values(locations) ++
        fr"ON CONFLICT (location_id) DO NOTHING"

      private def value(location: Location.WithOptionalCreatedField): Fragment = location match
        case Location.WithOptionalCreatedField(id, longitude, latitude, Some(created)) =>
          sql"(CAST($id AS VARCHAR), CAST($longitude AS NUMERIC), CAST($latitude AS NUMERIC), CAST($created AS TIMESTAMP))"
        case Location.WithOptionalCreatedField(id, longitude, latitude, None) =>
          sql"(CAST($id AS VARCHAR), CAST($longitude AS NUMERIC), CAST($latitude AS NUMERIC), CURRENT_TIMESTAMP)"
      
      private def values(locations: List[Location.WithOptionalCreatedField]): Fragment =
        locations.map(value).foldSmash(fr"VALUES", fr",", fr"")

    object update:
      def locations(locations: List[Location.WithoutCreatedField]): Either[Throwable, Update0] =
        locations match
          case List() => ServerError.IllegalArgument("Location list must not be empty").asLeft
          case _ => sqlScript(locations).update.asRight

      private def sqlScript(locations: List[Location.WithoutCreatedField]): Fragment =
        fr"UPDATE locations AS l" ++
        fr"SET location_longitude = loc.longitude, location_latitude = loc.latitude" ++
        fr"FROM (" ++
        update.values(locations) ++
        fr") AS loc (id, longitude, latitude)" ++
        fr"WHERE l.location_id = loc.id"

      private def value(location: Location.WithoutCreatedField): Fragment =
        val Location.WithoutCreatedField(id, longitude, latitude) = location
        sql"(CAST($id AS VARCHAR), CAST($longitude AS NUMERIC), CAST($latitude AS NUMERIC))"
      
      private def values(locations: List[Location.WithoutCreatedField]): Fragment =
        locations.map(value).foldSmash(fr"VALUES", fr",", fr"")


    object delete:
      def locations(ids: Location.Ids): Either[Throwable ,Update0] =
        ids match
          case List() => ServerError.IllegalArgument("Ids list must not be empty").asLeft
          case _ => sqlScript(ids).update.asRight

      private def sqlScript(ids: Location.Ids): Fragment = 
        fr"DELETE FROM locations" ++
          Fragments.whereAndOpt(select.byIds(ids))

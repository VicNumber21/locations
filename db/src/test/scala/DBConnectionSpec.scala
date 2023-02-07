import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import org.scalactic.Snapshots._
import scala.util.Random
import java.io.File

import com.dimafeng.testcontainers.DockerComposeContainer
import com.dimafeng.testcontainers.ExposedService
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.testcontainers.containers.wait.strategy.Wait
import com.dimafeng.testcontainers.ContainerDef
import com.dimafeng.testcontainers.WaitingForService

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.math.BigDecimal

import doobie._
import doobie.implicits._
import doobie.implicits.javasql._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.postgres.pgisimplicits._
import cats._
import cats.implicits._
import cats.effect._
import cats.effect.implicits._
import cats.effect.unsafe.implicits.global

import doobie.scalatest.IOChecker


object DbEnv {
  val portBegin = 5000;
  val portEnd = 5999;
  val rnd = new Random;
  val port = portBegin + rnd.nextInt(portEnd - portBegin + 1)

  val random =
    Map(
      "POSTGRES_USER" -> s"user_${Random.alphanumeric.take(10).mkString}" ,
      "POSTGRES_PASSWORD" -> s"pswd_${Random.alphanumeric.take(10).mkString}",
      "POSTGRES_DB_PATH" -> s"/tmp/${Random.alphanumeric.take(10).mkString}",
      "POSTGRES_DB_NAME" -> "locations",
      "POSTGRES_PORT" -> s"${port}"
    )

  val transactor =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      s"jdbc:postgresql://localhost:${port}/locations",
      "locator",
      "locator"
    )
}

case class Location(id: String, longitude: BigDecimal, latitude: BigDecimal, created: Timestamp)


class DBConnectionSpec extends AnyFlatSpec with TestContainerForAll with IOChecker {
  override val containerDef =
    DockerComposeContainer.Def(
      composeFiles = new File("./docker/compose.yaml"),
      env = DbEnv.random,
      waitingFor = Some(WaitingForService("db_1", Wait.forLogMessage(".*database system is ready to accept connections.*", 2)))
    )

  val transactor = DbEnv.transactor

  "Location db" should "be empty by default" in {
    val query =
      sql"SELECT location_id, location_longitude, location_latitude, location_created FROM locations"
        .query[Location]

    check(query)

    val result = query
    .to[List]
    .transact(DbEnv.transactor)
    .unsafeRunSync()

    println(snap(result).lines)
    result shouldBe empty
  }
}
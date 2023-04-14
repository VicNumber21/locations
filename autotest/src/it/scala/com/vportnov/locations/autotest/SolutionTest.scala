package com.vportnov.locations.autotest

import org.scalatest.{ DoNotDiscover, BeforeAndAfterEach }
import org.scalatest.matchers.should.Matchers._
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._

import org.http4s._
import org.http4s.implicits._
import org.http4s.headers._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.circe._

import io.circe._
import io.circe.syntax._

import cats.implicits._
import cats.effect.IO
import cats.effect.unsafe.implicits.global

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor

import java.time.{ LocalDateTime, ZoneOffset, ZonedDateTime }
import org.scalatest.BeforeAndAfterEach


@DoNotDiscover
class SolutionTest extends AnyAutotestSpec with BeforeAndAfterEach:
  info("As a developer I need to check if whole solution works (integration test)")

  override protected def beforeEach(): Unit =
    cleanDb()

  override protected def afterEach(): Unit = 
    cleanDb()

  "GET locations" should "return no locations if database is empty" in {
    Given("database is empty")

    And("uri does not have extra parameters")
      val uri = Uri.fromString(url.Get).value

    When("request is send to the service")
      val ReplyWithBody(reply, body) = sendRequest(Request(method = Method.GET, uri = uri))

    Then("status code is Ok (200)")
      reply.status shouldBe Status.Ok

    And("Content-Type is application/json and UTF-8")
      reply.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json, Charset.`UTF-8`)

    And("Transfer-Encoding is Chunked")
      reply.headers.get[`Transfer-Encoding`].value.hasChunked shouldBe true

    And("body is empty Json array")
      body shouldBe Json.arr()
  }

  it should "return all locations in JSON array if database is not empty" in {
    Given("database is not empty")
      val databaseEntries = insertIntoDb(
        List (
          ("location123", 24.356, -7.654321, LocalDateTime.now()),
          ("location456", 180, -90, LocalDateTime.now()),
          ("location789", -180, 90, LocalDateTime.now())
        )
      )

    And("uri does not have extra parameters")
      val uri = Uri.fromString(url.Get).value

    
    When("request is send to the service")
      val ReplyWithBody(reply, body) = sendRequest(Request(method = Method.GET, uri = uri))

    Then("status code is Ok (200)")
      reply.status shouldBe Status.Ok

    And("Content-Type is application/json and UTF-8")
      reply.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json, Charset.`UTF-8`)

    And("Transfer-Encoding is Chunked")
      reply.headers.get[`Transfer-Encoding`].value.hasChunked shouldBe true

    And("body is Json array with all entries")
      body.toSortedListOfTuple4 shouldBe databaseEntries
  }

  private object url:
    def Get = s"http://localhost:${apps.apiPort}/api/v1.0/locations"

  private case class ReplyWithBody(reply: Response[IO], body: Json)

  private def sendRequest(request: Request[IO]) = EmberClientBuilder
    .default[IO]
    .build
    .use(client => client.stream(request).pure[IO])
    .map(stream => stream.map(reply => ReplyWithBody(reply, reply.as[Json].unsafeRunSync())))
    .unsafeRunSync()
    .compile
    .toList
    .unsafeRunSync()
    .head

  private def insertIntoDb(entries: List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]])
                          : List[(String, BigDecimal, BigDecimal, ZonedDateTime)] =
    Update[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]] (
      "INSERT INTO locations (location_id, location_longitude, location_latitude, location_created) values (?, ?, ?, ?)"
    )
      .updateMany(entries)
      .transact(transactor)
      .unsafeRunSync()
    entries.sortBy(_._1).map(l => (l._1, l._2, l._3, l._4.atZone(ZoneOffset.UTC)))

  private def cleanDb(): Unit =
    sql"DELETE FROM locations"
      .update
      .run
      .transact(transactor)
      .unsafeRunSync()

  private def transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO](apps.dbConfig.driver, apps.dbConfig.userUrl, apps.dbConfig.user.login, apps.dbConfig.user.password)

  extension (j: Json)
    def toSortedListOfTuple4: List[(String, BigDecimal, BigDecimal, ZonedDateTime)] =
      j.asArray.value.toList.map(j =>
        (
          j.findAllByKey("id").collectFirst(_.asString.value).value,
          j.findAllByKey("longitude").collectFirst(_.as[BigDecimal].value).value,
          j.findAllByKey("latitude").collectFirst(_.as[BigDecimal].value).value,
          j.findAllByKey("created").collectFirst(j => ZonedDateTime.parse(j.asString.value)).value
        )
      )
        .sortBy(_._1)

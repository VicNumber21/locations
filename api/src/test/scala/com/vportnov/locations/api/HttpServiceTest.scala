package com.vportnov.locations.api

import org.scalatest.matchers.should.Matchers._
import org.scalatest.GivenWhenThen
import org.scalatest.EitherValues._
import org.scalatest.OptionValues._
import org.scalatest.flatspec.AnyFlatSpec

import cats._
import cats.effect.{ IO, Sync }
import cats.effect.unsafe.implicits.global

import fs2.Stream

import org.http4s._
import org.http4s.implicits._
import org.http4s.headers._
import org.http4s.circe._

import io.circe._
import io.circe.syntax._

import java.time.{ LocalDateTime, ZoneOffset, ZonedDateTime }
import java.util.UUID

import com.vportnov.locations.model
import com.vportnov.locations.api.types.response


class HttpServiceTest extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need http service which provides http api to whole solution")

  trait TestStorage[F[_]: Sync] extends model.StorageExt[F]:
    override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[F] = Stream.empty
    override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[F] = Stream.empty
    override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[F] = Stream.empty
    override def locationStats(period: model.Period): LocationStatsStream[F] = Stream.empty
    override def deleteLocations(ids: model.Location.Ids): F[Int] = Sync[F].delay(0)


  "GET /locations" should "should return empty JSON array if nothing in storage" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    When("request is send to the service")
      val result = service.app.run(Request(Method.GET, uri)).unsafeRunSync()

    Then("status code is Ok (200)")
      result.status shouldBe Status.Ok

    And("Content-Type is application/json and UTF-8")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json, Charset.`UTF-8`)

    And("Transfer-Encoding is Chunked")
      result.headers.get[`Transfer-Encoding`].value.hasChunked shouldBe true

    And("body is empty Json array")
      result.as[Json].unsafeRunSync() shouldBe Json.arr()
  }

  it should "should return all entries in JSON array if storage is not empty and no filter given" in {
    Given("service is connected to storage where some locations exist ")
      val locationsInStorage =
        List(
          model.Location("location123", 24.356, -7.654321, LocalDateTime.now()),
          model.Location("location456", 180, -90, LocalDateTime.now()),
          model.Location("location789", -180, 90, LocalDateTime.now())
        )

      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          Stream.emits(locationsInStorage)
      } 

      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    When("request is send to the service")
      val result = service.app.run(Request(Method.GET, uri)).unsafeRunSync()

    Then("status code is Ok (200)")
      result.status shouldBe Status.Ok

    And("Content-Type is application/json and UTF-8")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json, Charset.`UTF-8`)

    And("Transfer-Encoding is Chunked")
      result.headers.get[`Transfer-Encoding`].value.hasChunked shouldBe true

    And("body is Json array with all entries")
      val body = result.as[Json].unsafeRunSync()
      body.isArray shouldBe true
      body.toListOfTuple4 shouldBe locationsInStorage.toListOfTuple4
  }

  it should "return Bad Request failure if both period and id filters are used" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has both period and id queries")
      val uri = apiUri(s"/locations?from=${nowInUtc}&id=location123")
    
    When("request is send to the service")
      val result = service.app.run(Request(Method.GET, uri)).unsafeRunSync()

    Then("status code is Bad Request (400)")
      result.status shouldBe Status.BadRequest

    And("Content-Type is application/json")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json)

    And("body is Json object with error description")
      val body = result.as[Json].unsafeRunSync()
      body.isObject shouldBe true
      val (code, message, errorId) = body.toErrorDescription
      code shouldBe Status.BadRequest.code
      message should not be empty
      errorId should not be empty
      noException should be thrownBy UUID.fromString(errorId)
  }

  def apiUri(path: String): Uri =
    Uri.fromString(s"/api/v1.0${path}").value
  
  def nowInUtc = LocalDateTime.now().atZone(ZoneOffset.UTC)

  extension (j: Json)
    def toListOfTuple4: List[(String, BigDecimal, BigDecimal, ZonedDateTime)] =
      j.asArray.value.toList.map(j =>
        (
          j.findAllByKey("id").collectFirst(_.asString.value).value,
          j.findAllByKey("longitude").collectFirst(_.as[BigDecimal].value).value,
          j.findAllByKey("latitude").collectFirst(_.as[BigDecimal].value).value,
          j.findAllByKey("created").collectFirst(j => ZonedDateTime.parse(j.asString.value)).value
        )
      )

    def toErrorDescription: (Int, String, String) =
      (
        j.findAllByKey("code").collectFirst(_.as[Int].value).value,
        j.findAllByKey("message").collectFirst(_.asString.value).value,
        j.findAllByKey("errorId").collectFirst(_.asString.value).value
      )
  
  extension (locations: List[model.Location.WithCreatedField])
    def toListOfTuple4: List[(String, BigDecimal, BigDecimal, ZonedDateTime)] = 
      locations.map(l => (l.id, l.longitude, l.latitude, l.created.atZone(ZoneOffset.UTC)))

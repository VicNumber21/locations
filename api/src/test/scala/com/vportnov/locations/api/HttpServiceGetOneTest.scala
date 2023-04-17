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
import org.http4s.Method._
import org.http4s.implicits._
import org.http4s.headers._
import org.http4s.circe._
import org.http4s.client.dsl.io._

import io.circe._
import io.circe.syntax._

import java.time.{ LocalDateTime, ZoneOffset, ZonedDateTime }
import java.util.UUID

import com.vportnov.locations.model


class HttpServiceGetOneTest extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need http service which provides http api (GET ONE) to whole solution")

  trait TestStorage[F[_]: Sync] extends model.StorageExt[F]:
    override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[F] = ???
    override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[F] = Stream.empty
    override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[F] = ???
    override def locationStats(period: model.Period): LocationStatsStream[F] = ???
    override def deleteLocations(ids: model.Location.Ids): F[Int] = ???

  "GET /locations/:id" should "fail with Not Found if nothing in storage" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("status code is Not Found (404)")
      result.status shouldBe Status.NotFound

    And("Content-Type is application/json")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json)

    And("body is Json object with error description")
      val body = result.as[Json].unsafeRunSync()
      body.isObject shouldBe true
      val (code, message, errorId) = body.toErrorDescription
      code shouldBe Status.NotFound.code
      message should not be empty
      errorId should not be empty
      noException should be thrownBy UUID.fromString(errorId)
  }

  it should "return requested location if storage returns it" in {
    Given("service is connected to storage where needed location exists")
      val locationsInStorage =
        List(
          model.Location("location123", 24.356, -7.654321, LocalDateTime.now())
        )

      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          Stream.emits(locationsInStorage)
      } 

      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has the needed location id")
      val requestedId = "location123"
      val uri = apiUri(s"/locations/${requestedId}")
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("status code is Ok (200)")
      result.status shouldBe Status.Ok

    And("Content-Type is application/json")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json)

    And("body is requested location as Json object")
      val body = result.as[Json].unsafeRunSync()
      body.isObject shouldBe true
      body.toTuple4 shouldBe locationsInStorage.toListOfTuple4.head
  }

  it should "forward alphanumeric id to storage method" in {
    Given("uri has alphanumeric id")
      val id = "location123"
      val uri = apiUri(s"/locations/${id}")

    And("service is connected to storage which expect to get such parameters")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          period shouldBe empty
          ids shouldBe List(id)
          super.getLocations(period, ids)
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("all expectations are met")
      result.status shouldBe Status.NotFound
  }

  it should "fail with Bad Request if id is not alpanumeric string" in {
    Given("uri has id which is not alphanumeric string")
      val uri = apiUri("/locations/location-123")

    And("service is connected to empty storage")
      val storage = new TestStorage[IO] {}
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

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

  it should "fail with Internal Server Error if service method throws exception" in {
    Given("service is connected to storage which throws exception")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          throw new RuntimeException("Unexpected error")
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")

    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("status code is Internal Server Error (500)")
      result.status shouldBe Status.InternalServerError

    And("Content-Type is application/json")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json)

    And("body is Json object with error description")
      val body = result.as[Json].unsafeRunSync()
      body.isObject shouldBe true
      val (code, message, errorId) = body.toErrorDescription
      code shouldBe Status.InternalServerError.code
      errorId should not be empty
      noException should be thrownBy UUID.fromString(errorId)

    And("message does not leak too much details")
      message shouldBe "Internal Server Error"
  }

  it should "fail with Internal Server Error if service method generates stream with raised error" in {
    Given("service is connected to storage which generates stream with error")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          Stream.raiseError(new RuntimeException("Unexpected error"))
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("status code is Internal Server Error (500)")
      result.status shouldBe Status.InternalServerError

    And("Content-Type is application/json")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json)

    And("body is Json object with error description")
      val body = result.as[Json].unsafeRunSync()
      body.isObject shouldBe true
      val (code, message, errorId) = body.toErrorDescription
      code shouldBe Status.InternalServerError.code
      errorId should not be empty
      noException should be thrownBy UUID.fromString(errorId)

    And("message does not leak too much details")
      message shouldBe "Internal Server Error"
  }

  it should "fail with BadRequest if service method generates stream with raised IllegalArgumentException" in {
    Given("service is connected to storage which generates stream with IllegalArgumentException")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          Stream.raiseError(new IllegalArgumentException("Bad parameter"))
      } 

      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

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
  
  extension (j: Json)
    def toTuple4: (String, BigDecimal, BigDecimal, ZonedDateTime) =
      (
        j.findAllByKey("id").collectFirst(_.asString.value).value,
        j.findAllByKey("longitude").collectFirst(_.as[BigDecimal].value).value,
        j.findAllByKey("latitude").collectFirst(_.as[BigDecimal].value).value,
        j.findAllByKey("created").collectFirst(j => ZonedDateTime.parse(j.asString.value)).value
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

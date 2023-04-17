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


class HttpServicePostOneTest extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need http service which provides http api (POST ONE) to whole solution")

  trait TestStorage[F[_]: Sync] extends model.StorageExt[F]:
    override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[F] = Stream.empty
    override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[F] = ???
    override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[F] = ???
    override def locationStats(period: model.Period): LocationStatsStream[F] = ???
    override def deleteLocations(ids: model.Location.Ids): F[Int] = ???

  "POST /locations/:id" should "fail with Bad Request if no body given" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is empty")
      val requestBody = ""
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  it should "fail with Bad Request if not JSON is given as body" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is text")
      val requestBody = "Some text body"
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  it should "fail with Bad Request if wrong JSON object is given as body" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is JSON array with wrong object")
      val requestBody = Json.obj("key" := "v1", "key2" := "one")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  it should "fail with Bad Request if location id is not alphanumeric in uri" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has invalid location id")
      val uri = apiUri("/locations/location-123")
    
    And("request body is valid JSON object")
      val requestBody = Json.obj("longitude" := 0, "latitude" := 0, "created" := s"${nowAtUtc}")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  it should "ignore location id value in JSON object sent as body" in {
    Given("service is connected to storage which expects to get location id from uri")
      val expectedId = "location123"
      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          locations.head.id == expectedId
          super.createLocations(locations)

      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri(s"/locations/${expectedId}")
    
    And("request body is JSON object with invalid id")
      val requestBody = Json.obj("id" := "invalid-id", "longitude" := 0, "latitude" := 0, "created" := s"${nowAtUtc}")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

    Then("all expectation are met")
      result.status shouldBe Status.Conflict
  }

  it should "fail with Bad Request if longitude < -180" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is JSON object with longitude < -180")
      val requestBody = Json.obj("longitude" := -185, "latitude" := 0, "created" := s"${nowAtUtc}")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  it should "fail with Bad Request if longitude > 180" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is JSON object with longitude > 180")
      val requestBody = Json.obj("longitude" := 185, "latitude" := 0, "created" := s"${nowAtUtc}")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  it should "fail with Bad Request if latitude < -90" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is JSON object with latitude < -90")
      val requestBody = Json.obj("longitude" := 0, "latitude" := -95, "created" := s"${nowAtUtc}")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  it should "fail with Bad Request if latitude > 90" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is JSON object with latitude > 90")
      val requestBody = Json.obj("longitude" := 0, "latitude" := 95, "created" := s"${nowAtUtc}")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  it should "fail with Bad Request if 'created' is not ISO Date Time at UTC" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is JSON object with 'created' as ISO Date Time but not at UTC")
      val requestBody = Json.obj("longitude" := 0, "latitude" := 0, "created" := s"${LocalDateTime.now}")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  it should "fail with Bad Request if longitude is missed" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is JSON object without longitude")
      val requestBody = Json.obj( "latitude" := 0, "created" := s"${nowAtUtc}")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  it should "fail with Bad Request if latitude is missed" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is JSON object without latitude")
      val requestBody = Json.obj("longitude" := 0, "created" := s"${nowAtUtc}")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  it should "forward body data if 'created' is missed" in {
    Given("service is connected to storage which expects to get location object")
      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          locations shouldBe List(model.Location("location123", 0, 0, None))
          super.createLocations(locations)
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is location object without 'created'")
      val requestBody = Json.obj("longitude" := 0, "latitude" := 0)
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

    Then("expected locations forwarded to storage method")
      result.status shouldBe Status.Conflict
  }

  it should "forward body data if location is valid" in {
    Given("service is connected to storage which expects to get location object")
      val expectedCreated = nowAtUtc
      val expectedLocation = model.Location("location123", 34.4567, -65.098765, Some(expectedCreated.toLocalDateTime))
      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          locations shouldBe List(expectedLocation)
          super.createLocations(locations)
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has the location id from expected location")
      val uri = apiUri(s"/locations/${expectedLocation.id}")
    
    And("request body is location object with filed value from expected location")
      val requestBody =
        Json.obj(
          "longitude" := expectedLocation.longitude,
          "latitude" := expectedLocation.latitude,
          "created" := s"${expectedCreated}"
        )
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

    Then("expected locations forwarded to storage method")
      result.status shouldBe Status.Conflict
  }

  it should "fail with Conflict if sstream is generated by storage method" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is valid JSON object")
      val requestBody = Json.obj("longitude" := 0, "latitude" := 0, "created" := s"${nowAtUtc}")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

    Then("status code is Conflict (409)")
      result.status shouldBe Status.Conflict

    And("Content-Type is application/json")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json)

    And("body is Json object with error description")
      val body = result.as[Json].unsafeRunSync()
      body.isObject shouldBe true
      val (code, message, errorId) = body.toErrorDescription
      code shouldBe Status.Conflict.code
      message should not be empty
      errorId should not be empty
      noException should be thrownBy UUID.fromString(errorId)
  }

  it should "return created location if storage method returns it" in {
    Given("service is connected to storage where some locations exist ")
      val createdLocation = model.Location("location123", 24.356, -7.654321, LocalDateTime.now())

      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          Stream.emit(createdLocation)
      } 

      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is JSON array with valid location object")
      val requestBody =
        Json.obj(
          "longitude" := createdLocation.longitude,
          "latitude" := createdLocation.latitude,
          "created" := s"${createdLocation.created.atZone(ZoneOffset.UTC)}"
        )
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

    Then("status code is Created (201)")
      result.status shouldBe Status.Created

    And("Content-Type is application/json")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json)

    And("body is requested location as Json object")
      val body = result.as[Json].unsafeRunSync()
      body.isObject shouldBe true
      body.toTuple4 shouldBe createdLocation.toTuple4
  }

  it should "fail with Internal Server Error if service method throws exception" in {
    Given("service is connected to storage which throws exception")
      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          throw new RuntimeException("Unexpected error")
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is JSON array with valid location object")
      val requestBody = Json.obj("longitude" := 0, "latitude" := 0)
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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
    Given("service is connected to storage which throws exception")
      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          Stream.raiseError(new RuntimeException("Unexpected error"))
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")
    
    And("request body is JSON array with valid location object")
      val requestBody = Json.obj("longitude" := 0, "latitude" := 0)
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

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

  def apiUri(path: String): Uri =
    Uri.fromString(s"/api/v1.0${path}").value
  
  def nowAtUtc = LocalDateTime.now().atZone(ZoneOffset.UTC)

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
  
  extension (l: model.Location.WithCreatedField)
    def toTuple4: (String, BigDecimal, BigDecimal, ZonedDateTime) = 
      (l.id, l.longitude, l.latitude, l.created.atZone(ZoneOffset.UTC))

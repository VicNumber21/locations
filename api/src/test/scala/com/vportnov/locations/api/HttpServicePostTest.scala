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


class HttpServicePostTest extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need http service which provides http api (POST) to whole solution")

  trait TestStorage[F[_]: Sync] extends model.StorageExt[F]:
    override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[F] = Stream.empty
    override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[F] = ???
    override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[F] = ???
    override def locationStats(period: model.Period): LocationStatsStream[F] = ???
    override def deleteLocations(ids: model.Location.Ids): F[Int] = ???

  "POST /locations" should "fail with Bad Request if empty JSON array is given as body" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is empty JSON array")
      val requestBody = Json.arr()
    
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

  it should "fail with Bad Request if no body given" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri)).unsafeRunSync()

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
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
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

  it should "fail with Bad Request if JSON array with wrong objects is given as body" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with wrong objects")
      val requestBody = Json.arr(
        Json.obj("key" := "v1", "key2" := "one"),
        Json.obj("key" := "v2", "key2" := "two"),
        Json.obj("key" := "v3", "key2" := "three"),
      )
    
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

  it should "fail with Bad Request if location id is not alphanumeric" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with location object with malformed id")
      val requestBody = Json.arr(
        Json.obj("id" := "location-123", "longitude" := 0, "latitude" := 0, "created" := s"${nowAtUtc}")
      )
    
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

  it should "fail with Bad Request if longitude < -180" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with location object with longitude < -180")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "longitude" := -185, "latitude" := 0, "created" := s"${nowAtUtc}")
      )
    
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
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with location object with longitude > 180")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "longitude" := 185, "latitude" := 0, "created" := s"${nowAtUtc}")
      )
    
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
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with location object with latitude < -90")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "longitude" := 0, "latitude" := -95, "created" := s"${nowAtUtc}")
      )
    
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
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with location object with latitude > 90")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "longitude" := 0, "latitude" := 95, "created" := s"${nowAtUtc}")
      )
    
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
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with location object with 'created' as ISO Date Time but not at UTC")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "longitude" := 0, "latitude" := 0, "created" := s"${LocalDateTime.now}")
      )
    
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

  it should "fail with Bad Request if id is missed" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with location object without id")
      val requestBody = Json.arr(
        Json.obj("longitude" := 0, "latitude" := 0, "created" := s"${nowAtUtc}")
      )
    
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
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with location object without longitude")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "latitude" := 0, "created" := s"${nowAtUtc}")
      )
    
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
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with location object without latitude")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "longitude" := 0, "created" := s"${nowAtUtc}")
      )
    
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
    Given("service is connected to storage which expects to get location objects")
      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          locations shouldBe List(model.Location("location123", 0, 0, None))
          super.createLocations(locations)
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with location object without 'created'")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "longitude" := 0, "latitude" := 0)
      )
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

    Then("expected locations forwarded to storage method and status code is Created (201)")
      result.status shouldBe Status.Created
  }

  it should "forward body data to storage method if all locations given in right format with right values" in {
    Given("service is connected to storage which expects to get location objects")
      val expectedLocations =
        List(
          model.Location("001", 0, 0, Some(nowAtUtc.toLocalDateTime)),
          model.Location("002", 180, -90, Some(nowAtUtc.toLocalDateTime)),
          model.Location("003", -180, 90, Some(nowAtUtc.toLocalDateTime)),
          model.Location("003", -12.345678, 89.765432, Some(nowAtUtc.toLocalDateTime))
        )

      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          locations shouldBe expectedLocations
          super.createLocations(locations)
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with location object without latitude")
      val jsonLocations = expectedLocations.map { l =>
          Json.obj(
            "id" := l.id,
            "longitude" := l.longitude,
            "latitude" := l.latitude,
            "created" := s"${l.created.value.atZone(ZoneOffset.UTC)}"
          )
        }

      val requestBody = Json.arr(jsonLocations : _*)
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

    Then("expected locations forwarded to storage method and status code is Created (201)")
      result.status shouldBe Status.Created
  }

  it should "return empty JSON array if empty stream is generated by storage method" in {
    Given("service is connected to empty storage")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with valid location object")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "longitude" := 0, "latitude" := 0)
      )
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

    Then("status code is Created (201)")
      result.status shouldBe Status.Created

    And("Content-Type is application/json and UTF-8")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json, Charset.`UTF-8`)

    And("Transfer-Encoding is Chunked")
      result.headers.get[`Transfer-Encoding`].value.hasChunked shouldBe true

    And("body is empty Json array")
      result.as[Json].unsafeRunSync() shouldBe Json.arr()
  }

  it should "return all entries in JSON array if storage is not empty" in {
    Given("service is connected to storage where some locations exist ")
      val locationsInStorage =
        List(
          model.Location("location123", 24.356, -7.654321, LocalDateTime.now()),
          model.Location("location456", 180, -90, LocalDateTime.now()),
          model.Location("location789", -180, 90, LocalDateTime.now())
        )

      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          Stream.emits(locationsInStorage)
      } 

      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with valid location object")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "longitude" := 0, "latitude" := 0)
      )
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

    Then("status code is Created (201)")
      result.status shouldBe Status.Created

    And("Content-Type is application/json and UTF-8")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json, Charset.`UTF-8`)

    And("Transfer-Encoding is Chunked")
      result.headers.get[`Transfer-Encoding`].value.hasChunked shouldBe true

    And("body is Json array with all entries")
      val body = result.as[Json].unsafeRunSync()
      body.isArray shouldBe true
      body.toListOfTuple4 shouldBe locationsInStorage.toListOfTuple4
  }

  it should "fail with Internal Server Error if service method throws exception" in {
    Given("service is connected to storage which throws exception")
      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          throw new RuntimeException("Unexpected error")
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with valid location object")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "longitude" := 0, "latitude" := 0)
      )
    
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

  it should "succeed with Created but Internal Server Error is sent in JSON array if service method generates stream with raised error" in {
    Given("service is connected to storage which generates stream with error")
      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          Stream.raiseError(new RuntimeException("Unexpected error"))
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    And("request body is JSON array with valid location object")
      val requestBody = Json.arr(
        Json.obj("id" := "location123", "longitude" := 0, "latitude" := 0)
      )
    
    When("request is send to the service")
      val result = service.app.run(POST(uri = uri, body = requestBody)).unsafeRunSync()

    Then("status code is Created (201)")
      result.status shouldBe Status.Created

    And("Content-Type is application/json and UTF-8")
      result.headers.get[`Content-Type`].value shouldBe `Content-Type`(MediaType.application.json, Charset.`UTF-8`)

    And("Transfer-Encoding is Chunked")
      result.headers.get[`Transfer-Encoding`].value.hasChunked shouldBe true

    And("body is Json array with error object")
      val body = result.as[Json].unsafeRunSync()
      body.isArray shouldBe true
      val bodyArray = body.asArray.value
      bodyArray should have length 1
      val (code, message, errorId) = bodyArray.head.toErrorDescription
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

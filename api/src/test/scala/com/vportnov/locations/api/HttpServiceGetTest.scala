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
import com.vportnov.locations.api.types.response


class HttpServiceGetTest extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need http service which provides http api (GET) to whole solution")

  trait TestStorage[F[_]: Sync] extends model.StorageExt[F]:
    override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[F] = ???
    override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[F] = Stream.empty
    override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[F] = ???
    override def locationStats(period: model.Period): LocationStatsStream[F] = ???
    override def deleteLocations(ids: model.Location.Ids): F[Int] = ???

  "GET /locations" should "return empty JSON array if nothing in storage" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("status code is Ok (200)")
      result.status shouldBe Status.Ok

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
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          Stream.emits(locationsInStorage)
      } 

      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

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

  it should "fail with Bad Request if both period and id filters are used" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has both period and id queries")
      val uri = apiUri(s"/locations?from=${nowAtUtc}&id=location123")
    
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

  it should "forward empty period and empty id list to storage method" in {
    Given("uri does not have extra parameters (period and id list are empty)")
      val uri = apiUri("/locations")

    And("service is connected to storage which expect to get such parameters")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          period shouldBe empty
          ids shouldBe empty
          super.getLocations(period, ids)
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("all expectations are met")
      result.status shouldBe Status.Ok
  }

  it should "forward period with 'from' and empty id list to storage method" in {
    Given("uri has 'from' set in period and empty id list")
      val from = nowAtUtc
      val uri = apiUri(s"/locations?from=${from}")

    And("service is connected to storage which expect to get such parameters")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          period shouldBe model.Period(Some(from.toLocalDateTime()), None)
          ids shouldBe empty
          super.getLocations(period, ids)
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("all expectations are met")
      result.status shouldBe Status.Ok
  }

  it should "forward period with 'to' and empty id list to storage method" in {
    Given("uri has 'to' set in period and empty id list")
      val to = nowAtUtc
      val uri = apiUri(s"/locations?to=${to}")

    And("service is connected to storage which expect to get such parameters")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          period shouldBe model.Period(None, Some(to.toLocalDateTime()))
          ids shouldBe empty
          super.getLocations(period, ids)
      }

      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("all expectations are met")
      result.status shouldBe Status.Ok
  }

  it should "forward period with 'from' and 'to' ('from' < 'to') and empty id list to storage method" in {
    Given("uri has 'from' and 'to' ('from' < 'to') set in period and empty id list")
      val from = nowAtUtc
      val to = from.plusDays(3)
      val uri = apiUri(s"/locations?from=${from}&to=${to}")

    And("service is connected to storage which expect to get such parameters")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          period shouldBe model.Period(Some(from.toLocalDateTime()), Some(to.toLocalDateTime()))
          ids shouldBe empty
          super.getLocations(period, ids)
      }

      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("all expectations are met")
      result.status shouldBe Status.Ok
  }

  it should "forward period with 'from' and 'to' ('from' == 'to', just date count) and empty id list to storage method" in {
    Given("uri has 'from' and 'to' ('from' == 'to', just date count) set in period and empty id list")
      val from = nowAtUtc.toLocalDate().atStartOfDay().plusHours(12).atZone(ZoneOffset.UTC)
      val to = from.minusHours(3)
      val uri = apiUri(s"/locations?from=${from}&to=${to}")

    And("service is connected to storage which expect to get such parameters")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          period shouldBe model.Period(Some(from.toLocalDateTime()), Some(to.toLocalDateTime()))
          ids shouldBe empty
          super.getLocations(period, ids)
      }

      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("all expectations are met")
      result.status shouldBe Status.Ok
  }

  it should "fail with Bad Request if period with 'from' and 'to' ('from' > 'to') and empty id list are in parameters" in {
    Given("uri has 'from' and 'to' ('from' > 'to') set in period and empty id list")
      val from = nowAtUtc
      val to = from.minusDays(3)
      val uri = apiUri(s"/locations?from=${from}&to=${to}")

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

  it should "fail with Bad Request if 'from' set as empty string in period and id list is empty in parameters" in {
    Given("uri has 'from' as empty string in period and empty id list")
      val uri = apiUri("/locations?from=")

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

  it should "fail with Bad Request if 'from' set as not ISO Date Time string in period and id list is empty in parameters" in {
    Given("uri has 'from' as not ISO time string in period and empty id list")
      val uri = apiUri("/locations?from=location")

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

  it should "fail with Bad Request if 'from' set as not UTC in ISO Date Time string in period and id list is empty in parameters" in {
    Given("uri has 'from' as not UTC in ISO Date TIme time string in period and empty id list")
      val uri = apiUri(s"/locations?from=${LocalDateTime.now()}")

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

  it should "fail with Bad Request if 'to' set as empty string in period and id list is empty in parameters" in {
    Given("uri has 'to' as empty string in period and empty id list")
      val uri = apiUri("/locations?to=")

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

  it should "fail with Bad Request if 'to' set as not ISO Date Time string in period and id list is empty in parameters" in {
    Given("uri has 'to' as not ISO time string in period and empty id list")
      val uri = apiUri("/locations?to=location")

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

  it should "fail with Bad Request if 'to' set as not UTC in ISO Date Time string in period and id list is empty in parameters" in {
    Given("uri has 'to' as not UTC in ISO Date TIme time string in period and empty id list")
      val uri = apiUri(s"/locations?to=${LocalDateTime.now()}")

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

  it should "forward empty period and single alphanumeric id to storage method" in {
    Given("uri has empty period and single alphanumeric id")
      val id = "location123"
      val uri = apiUri(s"/locations?id=${id}")

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
      result.status shouldBe Status.Ok
  }

  it should "forward empty period and multiple alphanumeric ids to storage method" in {
    Given("uri has empty period and multiple alphanumeric ids")
      val id1 = "location123"
      val id2 = "location456"
      val id3 = "location789"
      val uri = apiUri(s"/locations?id=${id1}&id=${id2}&id=${id3}")

    And("service is connected to storage which expect to get such parameters")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          period shouldBe empty
          ids shouldBe List(id1, id2, id3)
          super.getLocations(period, ids)
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("all expectations are met")
      result.status shouldBe Status.Ok
  }

  it should "fail with Bad Request if period is empty and id is empty string in parameters" in {
    Given("uri has empty period and id which is empty string")
      val uri = apiUri("/locations?id=")

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

  it should "fail with Bad Request if period is empty and id is not alpanumeric string in parameters" in {
    Given("uri has empty period and id which is not alphanumeric string")
      val uri = apiUri("/locations?id=location-123")

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
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")

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

  it should "succeed with Ok but Internal Server Error is sent in JSON array if service method generates stream with raised error" in {
    Given("service is connected to storage which generates stream with error")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          Stream.raiseError(new RuntimeException("Unexpected error"))
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("status code is Ok (200)")
      result.status shouldBe Status.Ok

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

  it should "succeed with Ok but Bad Request is sent in JSON array if service method generates stream with raised IllegalArgumentException" in {
    Given("service is connected to storage which generates stream with IllegalArgumentException")
      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[IO] =
          Stream.raiseError(new IllegalArgumentException("Bad parameter"))
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri does not have extra parameters")
      val uri = apiUri("/locations")
    
    When("request is send to the service")
      val result = service.app.run(GET(uri)).unsafeRunSync()

    Then("status code is Ok (200)")
      result.status shouldBe Status.Ok

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
      code shouldBe Status.BadRequest.code
      message should not be empty
      errorId should not be empty
      noException should be thrownBy UUID.fromString(errorId)
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

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

import java.util.UUID

import com.vportnov.locations.model


class HttpServiceDeleteOneTest extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need http service which provides http api (DELETE ONE) to whole solution")

  trait TestStorage[F[_]: Sync] extends model.StorageExt[F]:
    override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[F] = ???
    override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[F] = ???
    override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[F] = ???
    override def locationStats(period: model.Period): LocationStatsStream[F] = ???
    override def deleteLocations(ids: model.Location.Ids): F[Int] = Sync[F].delay(0)

  "DELETE /locations/:id" should "return Ok if storage reports that nothing was deleted" in {
    Given("service is connected to storage where no location exists ")
      val storage = new TestStorage[IO] {} 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has some valid location id")
      val uri = apiUri("/locations/location123")
    
    When("request is send to the service")
      val result = service.app.run(DELETE(uri)).unsafeRunSync()

    Then("status code is Ok (200)")
      result.status shouldBe Status.Ok

    And("body is empty")
      result.as[String].unsafeRunSync() shouldBe empty
  }

  it should "return No Content if storage reports that location was deleted" in {
    Given("service is connected to storage where the requested location exists ")
      val storage = new TestStorage[IO] {
        override def deleteLocations(ids: model.Location.Ids): IO[Int] = IO.delay(1)
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has some valid location ids")
      val uri = apiUri("/locations/location123")
    
    When("request is send to the service")
      val result = service.app.run(DELETE(uri)).unsafeRunSync()

    Then("status code is NoContent (204)")
      result.status shouldBe Status.NoContent

    And("body is empty")
      result.as[String].unsafeRunSync() shouldBe empty
  }


  it should "forward valid location id to storage method" in {
    Given("uri has non empty list of location ids)")
      val expectedId = "location123"
      val uri = apiUri(s"/locations/${expectedId}")

    And("service is connected to storage which expect to get such parameters")
      val storage = new TestStorage[IO] {
        override def deleteLocations(ids: model.Location.Ids): IO[Int] =
          ids shouldBe List(expectedId)
          super.deleteLocations(ids)
      } 

      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    When("request is send to the service")
      val result = service.app.run(DELETE(uri)).unsafeRunSync()

    Then("all expectations are met")
      result.status shouldBe Status.Ok
  }

  it should "fail with Bad Request if id is not alpanumeric string" in {
    Given("uri has id which is not alphanumeric string")
      val uri = apiUri("/locations/location-123")

    And("service is connected to empty storage")
      val storage = new TestStorage[IO] {}
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    When("request is send to the service")
      val result = service.app.run(DELETE(uri)).unsafeRunSync()

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
        override def deleteLocations(ids: model.Location.Ids): IO[Int] =
          throw new RuntimeException("Unexpected error")
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")

    When("request is send to the service")
      val result = service.app.run(DELETE(uri)).unsafeRunSync()

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

  it should "fail with Internal Server Error if service method returns IO with raised error" in {
    Given("service is connected to storage which returns IO with raised error")
      val storage = new TestStorage[IO] {
        override def deleteLocations(ids: model.Location.Ids): IO[Int] =
          IO.raiseError(new RuntimeException("Unexpected error"))
      } 
      val service = new HttpService(storage, isSwaggerUIEnabled = false)
    
    And("uri has a valid location id")
      val uri = apiUri("/locations/location123")

    When("request is send to the service")
      val result = service.app.run(DELETE(uri)).unsafeRunSync()

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
  
  extension (j: Json)
    def toErrorDescription: (Int, String, String) =
      (
        j.findAllByKey("code").collectFirst(_.as[Int].value).value,
        j.findAllByKey("message").collectFirst(_.asString.value).value,
        j.findAllByKey("errorId").collectFirst(_.asString.value).value
      )

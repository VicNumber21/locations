package com.vportnov.locations.grpc.bindings

import org.scalatest.matchers.should.Matchers._
import org.scalatest.EitherValues._
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec

import java.time.LocalDateTime

import com.google.protobuf.timestamp.{ Timestamp => grpcTimestamp }

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.Stream

import com.vportnov.locations.model
import com.vportnov.locations.grpc
import com.vportnov.locations.utils


class bindingsTest extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need utilities to encode a data to grpc messages and decode the messages back to the data")

  "model.Location.Timestamp encoding and then decoding" should "get the same result" in {
    Given("an initial value")
      val initialValue: model.Location.Timestamp = LocalDateTime.now()
    
    When("the value is encoded to message")
      val message: grpcTimestamp = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Timestamp = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "model.Location.OptionalTimestamp encoding and then decoding" should "get the same result if timestamp is Some" in {
    Given("an initial value is Some")
      val initialValue: model.Location.OptionalTimestamp = Some(LocalDateTime.now())
    
    When("the value is encoded to message")
      val message: Option[grpcTimestamp] = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.OptionalTimestamp = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if timestamp is None" in {
    Given("an initial value is None")
      val initialValue: model.Location.OptionalTimestamp = None
    
    When("the value is encoded to message")
      val message: Option[grpcTimestamp] = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.OptionalTimestamp = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "model.Perioda encoding and then decoding" should "get the same result if period is empty" in {
    Given("an initial value is empty")
      val initialValue: model.Period = model.Period(None, None)
    
    When("the value is encoded to message")
      val message: grpc.Period = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Period = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "the same result if period has 'from' only" in {
    Given("an initial value has 'from' only")
      val initialValue: model.Period = model.Period(Some(LocalDateTime.now()), None)
    
    When("the value is encoded to message")
      val message: grpc.Period = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Period = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "the same result if period has 'to' only" in {
    Given("an initial value has 'to' only")
      val initialValue: model.Period = model.Period(None, Some(LocalDateTime.now()))
    
    When("the value is encoded to message")
      val message: grpc.Period = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Period = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "the same result if period has both 'from' and 'to'" in {
    Given("an initial value has both 'from' and 'to'")
      val initialValue: model.Period = model.Period(Some(LocalDateTime.now()), Some(LocalDateTime.now()))
    
    When("the value is encoded to message")
      val message: grpc.Period = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Period = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "model.Location.Ids encoding and then decoding" should "get the same result if ids list is empty" in {
    Given("an initial value is empty")
      val initialValue: model.Location.Ids = List()
    
    When("the value is encoded to message")
      val message: grpc.Ids = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Ids = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if ids list is not empty" in {
    Given("an initial value is not empty")
      val initialValue: model.Location.Ids = List("location123", "location456", "location789")
    
    When("the value is encoded to message")
      val message: grpc.Ids = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Ids = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get empty list if decoded from None (from Option[])" in {
    Given("message is None")
      val message: Option[grpc.Ids] = None
    
    When("the message is decoded to value")
      val decodedValue: model.Location.Ids = message.toModel
    
    Then("the value is empty list")
      decodedValue shouldBe List.empty[model.Location.Id]
  }

  it should "get empty list if decoded from Some (from Option[])" in {
    Given("an initial value is not empty")
      val initialValue: model.Location.Ids = List("location123", "location456", "location789")
    
    When("the value is encoded to message wrapped by Some (by Option[])")
      val message: Option[grpc.Ids] = Some(initialValue.toMessage)
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Ids = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "model.Location.Stats encoding and then decoding" should "get the same result" in {
    Given("an initial value is empty")
      val initialValue: model.Location.Stats = model.Location.Stats(LocalDateTime.now(), 7)
    
    When("the value is encoded to message")
      val message: grpc.LocationStats = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Stats = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "model.Location.Longitude encoding and then decoding" should "get the same result if the value is 0" in {
    Given("an initial value is 0")
      val initialValue: model.Location.Longitude = 0
    
    When("the value is encoded to message")
      val message: grpc.BigDecimal = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Longitude = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if the value is minimal (-180)" in {
    Given("an initial value is minimal(-180)")
      val initialValue: model.Location.Longitude = -180
    
    When("the value is encoded to message")
      val message: grpc.BigDecimal = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Longitude = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if the value is maximal (180)" in {
    Given("an initial value is maximal (180)")
      val initialValue: model.Location.Longitude = 180
    
    When("the value is encoded to message")
      val message: grpc.BigDecimal = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Longitude = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if the value is negative fraction" in {
    Given("an initial value is negative fraction")
      val initialValue: model.Location.Longitude = -45.123456
    
    When("the value is encoded to message")
      val message: grpc.BigDecimal = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Longitude = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if the value is positive fraction" in {
    Given("an initial value is positive fraction")
      val initialValue: model.Location.Longitude = 45.123456
    
    When("the value is encoded to message")
      val message: grpc.BigDecimal = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Longitude = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "model.Location.Latitude encoding and then decoding" should "get the same result if the value is 0" in {
    Given("an initial value is 0")
      val initialValue: model.Location.Latitude = 0
    
    When("the value is encoded to message")
      val message: grpc.BigDecimal = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Latitude = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if the value is minimal (-90)" in {
    Given("an initial value is minimal(-180)")
      val initialValue: model.Location.Latitude = -90
    
    When("the value is encoded to message")
      val message: grpc.BigDecimal = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Latitude = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if the value is maximal (90)" in {
    Given("an initial value is maximal (90)")
      val initialValue: model.Location.Latitude = 90
    
    When("the value is encoded to message")
      val message: grpc.BigDecimal = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Latitude = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if the value is negative fraction" in {
    Given("an initial value is negative fraction")
      val initialValue: model.Location.Latitude = -45.123456
    
    When("the value is encoded to message")
      val message: grpc.BigDecimal = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Latitude = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if the value is positive fraction" in {
    Given("an initial value is positive fraction")
      val initialValue: model.Location.Latitude = 45.123456
    
    When("the value is encoded to message")
      val message: grpc.BigDecimal = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Latitude = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "model.Location.WithCreatedField encoding and then decoding" should "get the same result" in {
    Given("an initial value")
      val initialValue: model.Location.WithCreatedField = model.Location("location123", 0, 0, LocalDateTime.now)
    
    When("the value is encoded to message")
      val message: grpc.Location = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.WithCreatedField = message.toLocationWithCreatedField
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "model.Location.WithOptionalCreatedField encoding and then decoding" should "get the same result if created is None" in {
    Given("an initial value")
      val initialValue: model.Location.WithOptionalCreatedField = model.Location("location123", 0, 0, None)
    
    When("the value is encoded to message")
      val message: grpc.Location = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.WithOptionalCreatedField = message.toLocationWithOptionalCreatedField
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if created is Some" in {
    Given("an initial value")
      val initialValue: model.Location.WithOptionalCreatedField = model.Location("location123", 0, 0, Some(LocalDateTime.now))
    
    When("the value is encoded to message")
      val message: grpc.Location = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.WithOptionalCreatedField = message.toLocationWithOptionalCreatedField
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "model.Location.WithoutCreatedField encoding and then decoding" should "get the same result" in {
    Given("an initial value")
      val initialValue: model.Location.WithoutCreatedField = model.Location("location123", 0, 0)
    
    When("the value is encoded to message")
      val message: grpc.Location = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.WithoutCreatedField = message.toLocationWithoutCreatedField
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "List of model.Location.WithCreatedField encoding and then decoding" should "get the same result if List is empty" in {
    Given("an initial value is empty list")
      val initialValue: List[model.Location.WithCreatedField] = List.empty[model.Location.WithCreatedField]
    
    When("the value is encoded to message")
      val message: grpc.Locations = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: List[model.Location.WithCreatedField] = message.toLocationsWithCreatedField
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if List is not empty" in {
    Given("an initial value is not empty list")
      val initialValue: List[model.Location.WithCreatedField] =
        List(
          model.Location("location123", 0, 0, LocalDateTime.now),
          model.Location("location456", 0, 0, LocalDateTime.now),
          model.Location("location789", 0, 0, LocalDateTime.now)
        )
    
    When("the value is encoded to message")
      val message: grpc.Locations = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: List[model.Location.WithCreatedField] = message.toLocationsWithCreatedField
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "List of model.Location.WithOptionalCreatedField encoding and then decoding" should "get the same result if List is empty" in {
    Given("an initial value is empty list")
      val initialValue: List[model.Location.WithOptionalCreatedField] = List.empty[model.Location.WithOptionalCreatedField]
    
    When("the value is encoded to message")
      val message: grpc.Locations = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: List[model.Location.WithOptionalCreatedField] = message.toLocationsWithOptionalCreatedField
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if List is not empty" in {
    Given("an initial value is not empty list")
      val initialValue: List[model.Location.WithOptionalCreatedField] =
        List(
          model.Location("location123", 0, 0, Some(LocalDateTime.now)),
          model.Location("location456", 0, 0, None),
          model.Location("location789", 0, 0, Some(LocalDateTime.now))
        )
    
    When("the value is encoded to message")
      val message: grpc.Locations = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: List[model.Location.WithOptionalCreatedField] = message.toLocationsWithOptionalCreatedField
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "List of model.Location.WithoutCreatedField encoding and then decoding" should "get the same result if List is empty" in {
    Given("an initial value is empty list")
      val initialValue: List[model.Location.WithoutCreatedField] = List.empty[model.Location.WithoutCreatedField]
    
    When("the value is encoded to message")
      val message: grpc.Locations = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: List[model.Location.WithoutCreatedField] = message.toLocationsWithoutCreatedField
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if List is not empty" in {
    Given("an initial value is not empty list")
      val initialValue: List[model.Location.WithoutCreatedField] =
        List(
          model.Location("location123", 0, 0),
          model.Location("location456", 0, 0),
          model.Location("location789", 0, 0)
        )
    
    When("the value is encoded to message")
      val message: grpc.Locations = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: List[model.Location.WithoutCreatedField] = message.toLocationsWithoutCreatedField
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  "ServerError.Kind encoding and then decoding" should "get the same result if kind is IllegalArgument" in {
    Given("an initial value is IllegalArgument")
      val initialValue: utils.ServerError.Kind = utils.ServerError.Kind.IllegalArgument
    
    When("the value is encoded to message")
      val message: grpc.ServerError.Kind = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: utils.ServerError.Kind = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if kind is NoSuchElement" in {
    Given("an initial value is NoSuchElement")
      val initialValue: utils.ServerError.Kind = utils.ServerError.Kind.NoSuchElement
    
    When("the value is encoded to message")
      val message: grpc.ServerError.Kind = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: utils.ServerError.Kind = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same result if kind is Internal" in {
    Given("an initial value is Internal")
      val initialValue: utils.ServerError.Kind = utils.ServerError.Kind.Internal
    
    When("the value is encoded to message")
      val message: grpc.ServerError.Kind = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: utils.ServerError.Kind = message.toModel
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get Internal if decoded from Unrecoginized" in {
    Given("the message is Unrecognized")
      val message: grpc.ServerError.Kind = grpc.ServerError.Kind.Unrecognized(10)
    
    When("the message is decoded back to value")
      val decodedValue: utils.ServerError.Kind = message.toModel
    
    Then("decoded value is equal to Internal")
      decodedValue shouldBe utils.ServerError.Kind.Internal
  }

  "ServerError encoding and then decoding" should "get the same REMOTE error if its kind is IllegalArgument" in {
    Given("an initial value is IllegalArgument")
      val initialValue: utils.ServerError = utils.ServerError.IllegalArgument("Bad thing")
    
    When("the value is encoded to message")
      val message: grpc.ServerError = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: utils.ServerError = message.toModel
    
    Then("decoded value is REMOTE version of initial value")
      decodedValue.message shouldBe initialValue.message
      decodedValue.kind shouldBe initialValue.kind
      decodedValue.uuid shouldBe initialValue.uuid
      decodedValue.getMessage.stripPrefix("REMOTE") shouldBe initialValue.getMessage.stripPrefix("LOCAL")
  }

  it should "get the same REMOTE error if its kind is NoSuchElement" in {
    Given("an initial value is NoSuchElement")
      val initialValue: utils.ServerError = utils.ServerError.NoSuchElement("Bad thing")
    
    When("the value is encoded to message")
      val message: grpc.ServerError = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: utils.ServerError = message.toModel
    
    Then("decoded value is REMOTE version of initial value")
      decodedValue.message shouldBe initialValue.message
      decodedValue.kind shouldBe initialValue.kind
      decodedValue.uuid shouldBe initialValue.uuid
      decodedValue.getMessage.stripPrefix("REMOTE") shouldBe initialValue.getMessage.stripPrefix("LOCAL")
  }

  it should "get the same REMOTE error if its kind is Internal" in {
    Given("an initial value is Internal")
      val initialValue: utils.ServerError = utils.ServerError.Internal("Bad thing")
    
    When("the value is encoded to message")
      val message: grpc.ServerError = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: utils.ServerError = message.toModel
    
    Then("decoded value is REMOTE version of initial value")
      decodedValue.message shouldBe initialValue.message
      decodedValue.kind shouldBe initialValue.kind
      decodedValue.uuid shouldBe initialValue.uuid
      decodedValue.getMessage.stripPrefix("REMOTE") shouldBe initialValue.getMessage.stripPrefix("LOCAL")
  }

  it should "get IllegalArgument error if converted from initial IllegalArgumentException" in {
    Given("an initial value is IllegalArgumentException")
      val exceptionMessage = "Bad argument"
      val initialValue: Throwable = new IllegalArgumentException(exceptionMessage)
    
    When("the value is encoded to message")
      val message: grpc.ServerError = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: utils.ServerError = message.toModel
    
    Then("decoded value is IllegalArgument and REMOTE")
      decodedValue.message should include (initialValue.getClass.getName)
      decodedValue.message should include (exceptionMessage)
      decodedValue.kind shouldBe utils.ServerError.Kind.IllegalArgument
      decodedValue.getMessage should include ("REMOTE")
      decodedValue.getMessage should include (utils.ServerError.Kind.IllegalArgument.toString)
      decodedValue.getMessage should include (decodedValue.uuid.toString)
      decodedValue.getMessage should include (exceptionMessage)
  }

  it should "get NoSuchElement error if converted from initial NoSuchElementException" in {
    Given("an initial value is NoSuchElementException")
      val exceptionMessage = "Empty collection"
      val initialValue: Throwable = new NoSuchElementException(exceptionMessage)
    
    When("the value is encoded to message")
      val message: grpc.ServerError = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: utils.ServerError = message.toModel
    
    Then("decoded value is NoSuchElement and REMOTE")
      decodedValue.message should include (initialValue.getClass.getName)
      decodedValue.message should include (exceptionMessage)
      decodedValue.kind shouldBe utils.ServerError.Kind.NoSuchElement
      decodedValue.getMessage should include ("REMOTE")
      decodedValue.getMessage should include (utils.ServerError.Kind.NoSuchElement.toString)
      decodedValue.getMessage should include (decodedValue.uuid.toString)
      decodedValue.getMessage should include (exceptionMessage)
  }

  it should "get Internal error if converted from initial exception other than IllegalArgumentException or NoSuchElementException" in {
    Given("an initial value is some exception but IllegalArgumentException or NoSuchElementException")
      val exceptionMessage = "Bad thing"
      val initialValue: Throwable = new RuntimeException(exceptionMessage)
    
    When("the value is encoded to message")
      val message: grpc.ServerError = initialValue.toMessage
    
    And("the message is decoded back to value")
      val decodedValue: utils.ServerError = message.toModel
    
    Then("decoded value is Internal and REMOTE")
      decodedValue.message should include (initialValue.getClass.getName)
      decodedValue.message should include (exceptionMessage)
      decodedValue.kind shouldBe utils.ServerError.Kind.Internal
      decodedValue.getMessage should include ("REMOTE")
      decodedValue.getMessage should include (utils.ServerError.Kind.Internal.toString)
      decodedValue.getMessage should include (decodedValue.uuid.toString)
      decodedValue.getMessage should include (exceptionMessage)
  }

  "IO[Int] encoding and then decoding" should "get the same successful result" in {
    Given("an initial value is successful result of IO[Int]")
      val initialValue: Int = 5
      val initialValueInEffect: IO[Int] = IO(initialValue)
    
    When("the value is encoded to message")
      val message: IO[grpc.CountReply] = initialValueInEffect.packCount
    
    And("the message is decoded back to value")
      val decodedValue: Int = message.unpackCount.unsafeRunSync()
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same failure result" in {
    Given("an initial value is failure result of IO[Int]")
      val initialValue: utils.ServerError = utils.ServerError.IllegalArgument("Bad argument")
      val initialValueInEffect: IO[Int] = IO.raiseError(initialValue)
    
    When("the value is encoded to message")
      val message: IO[grpc.CountReply] = initialValueInEffect.packCount
    
    And("the message is decoded back to value")
      a [utils.ServerError] should be thrownBy message.unpackCount.unsafeRunSync()
      val decodedValue: utils.ServerError = message.unpackCount.attempt.unsafeRunSync().left.value.asInstanceOf[utils.ServerError]
    
    Then("decoded value is equal to initial value")
      decodedValue.message shouldBe initialValue.message
      decodedValue.kind shouldBe initialValue.kind
      decodedValue.uuid shouldBe initialValue.uuid
      decodedValue.getMessage.stripPrefix("REMOTE") shouldBe initialValue.getMessage.stripPrefix("LOCAL")
  }

  it should "get local Internal ServerError if malformed message received" in {
    Given("a message is malformed")
      val message: IO[grpc.CountReply] = IO(grpc.CountReply())
    
    When("the message is decoded back to value")
      a [utils.ServerError] should be thrownBy message.unpackCount.unsafeRunSync()
      val decodedValue: utils.ServerError = message.unpackCount.attempt.unsafeRunSync().left.value.asInstanceOf[utils.ServerError]
    
    Then("decoded value is local Internal SeverError")
      decodedValue.message should include ("Incorrect format")
      decodedValue.message should include ("CountReply")
      decodedValue.kind shouldBe utils.ServerError.Kind.Internal
      decodedValue.getMessage should include ("LOCAL")
      decodedValue.getMessage should include (utils.ServerError.Kind.Internal.toString)
      decodedValue.getMessage should include (decodedValue.uuid.toString)
      decodedValue.getMessage should include ("Incorrect format")
      decodedValue.getMessage should include ("CountReply")
  }

  "Stream[IO, Location.Stats] encoding and then decoding" should "get the same successful result" in {
    Given("an initial value is successful result of Stream[IO, Location.Stats]")
      val initialValue: model.Location.Stats = model.Location.Stats(LocalDateTime.now(), 9)
      val initialValueInEffect: Stream[IO, model.Location.Stats]  = Stream(initialValue)
    
    When("the value is encoded to message")
      val message: Stream[IO, grpc.LocationStatsReply] = initialValueInEffect.packLocationStats
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.Stats = message.map(_.unpackLocationStats).compile.toList.unsafeRunSync().head
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same failure result" in {
    Given("an initial value is failure result of Stream[IO, Location.Stats]")
      val initialValue: utils.ServerError = utils.ServerError.NoSuchElement("Bad collection")
      val initialValueInEffect: Stream[IO, model.Location.Stats] = Stream.raiseError(initialValue)
    
    When("the value is encoded to message")
      val message: Stream[IO, grpc.LocationStatsReply] = initialValueInEffect.packLocationStats
    
    And("the message is decoded back to value")
      a [utils.ServerError] should be thrownBy message.map(_.unpackLocationStats).compile.toList.unsafeRunSync().head
      val decodedValue: utils.ServerError = message
        .map(_.unpackLocationStats)
        .attempt
        .compile.toList.unsafeRunSync().head
        .left.value.asInstanceOf[utils.ServerError]
    
    Then("decoded value is equal to initial value")
      decodedValue.message shouldBe initialValue.message
      decodedValue.kind shouldBe initialValue.kind
      decodedValue.uuid shouldBe initialValue.uuid
      decodedValue.getMessage.stripPrefix("REMOTE") shouldBe initialValue.getMessage.stripPrefix("LOCAL")
  }

  it should "get local Internal ServerError if malformed message received" in {
    Given("a message is malformed")
      val message: Stream[IO, grpc.LocationStatsReply] = Stream(grpc.LocationStatsReply())
    
    When("the message is decoded back to value")
      a [utils.ServerError] should be thrownBy message.map(_.unpackLocationStats).compile.toList.unsafeRunSync().head
      val decodedValue: utils.ServerError = message
        .map(_.unpackLocationStats)
        .attempt
        .compile.toList.unsafeRunSync().head
        .left.value.asInstanceOf[utils.ServerError]
    
    Then("decoded value is local Internal SeverError")
      decodedValue.message should include ("Incorrect format")
      decodedValue.message should include ("LocationStatsReply")
      decodedValue.kind shouldBe utils.ServerError.Kind.Internal
      decodedValue.getMessage should include ("LOCAL")
      decodedValue.getMessage should include (utils.ServerError.Kind.Internal.toString)
      decodedValue.getMessage should include (decodedValue.uuid.toString)
      decodedValue.getMessage should include ("Incorrect format")
      decodedValue.getMessage should include ("LocationStatsReply")
  }

  "Stream[IO, Location.WithCreatedField] encoding and then decoding" should "get the same successful result" in {
    Given("an initial value is successful result of Stream[IO, Location.WithCreatedField]")
      val initialValue: model.Location.WithCreatedField = model.Location("location123", 0, 0, LocalDateTime.now())
      val initialValueInEffect: Stream[IO, model.Location.WithCreatedField]  = Stream(initialValue)
    
    When("the value is encoded to message")
      val message: Stream[IO, grpc.LocationReply] = initialValueInEffect.packLocation
    
    And("the message is decoded back to value")
      val decodedValue: model.Location.WithCreatedField = message.map(_.unpackLocation).compile.toList.unsafeRunSync().head
    
    Then("decoded value is equal to initial value")
      decodedValue shouldBe initialValue
  }

  it should "get the same failure result" in {
    Given("an initial value is failure result of Stream[IO, Location.WithCreatedField]")
      val initialValue: utils.ServerError = utils.ServerError.Internal("Unexpected exception")
      val initialValueInEffect: Stream[IO, model.Location.WithCreatedField] = Stream.raiseError(initialValue)
    
    When("the value is encoded to message")
      val message: Stream[IO, grpc.LocationReply] = initialValueInEffect.packLocation
    
    And("the message is decoded back to value")
      a [utils.ServerError] should be thrownBy message.map(_.unpackLocation).compile.toList.unsafeRunSync().head
      val decodedValue: utils.ServerError = message
        .map(_.unpackLocation)
        .attempt
        .compile.toList.unsafeRunSync().head
        .left.value.asInstanceOf[utils.ServerError]
    
    Then("decoded value is equal to initial value")
      decodedValue.message shouldBe initialValue.message
      decodedValue.kind shouldBe initialValue.kind
      decodedValue.uuid shouldBe initialValue.uuid
      decodedValue.getMessage.stripPrefix("REMOTE") shouldBe initialValue.getMessage.stripPrefix("LOCAL")
  }

  it should "get local Internal ServerError if malformed message received" in {
    Given("a message is malformed")
      val message: Stream[IO, grpc.LocationReply] = Stream(grpc.LocationReply())
    
    When("the message is decoded back to value")
      a [utils.ServerError] should be thrownBy message.map(_.unpackLocation).compile.toList.unsafeRunSync().head
      val decodedValue: utils.ServerError = message
        .map(_.unpackLocation)
        .attempt
        .compile.toList.unsafeRunSync().head
        .left.value.asInstanceOf[utils.ServerError]
    
    Then("decoded value is local Internal SeverError")
      decodedValue.message should include ("Incorrect format")
      decodedValue.message should include ("LocationReply")
      decodedValue.kind shouldBe utils.ServerError.Kind.Internal
      decodedValue.getMessage should include ("LOCAL")
      decodedValue.getMessage should include (utils.ServerError.Kind.Internal.toString)
      decodedValue.getMessage should include (decodedValue.uuid.toString)
      decodedValue.getMessage should include ("Incorrect format")
      decodedValue.getMessage should include ("LocationReply")
  }

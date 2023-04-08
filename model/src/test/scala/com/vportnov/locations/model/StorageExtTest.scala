package com.vportnov.locations.model

import org.scalatest.matchers.should.Matchers._
import org.scalatest.EitherValues._
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec

import cats.effect._
import cats.effect.unsafe.implicits.global

import fs2.Stream

import java.time.LocalDateTime

import com.vportnov.locations.model.Location
import com.vportnov.locations.model.Location.Ids
import com.vportnov.locations.model.Location.WithoutCreatedField
import com.vportnov.locations.model.Location.WithOptionalCreatedField
import com.vportnov.locations.utils.ServerError


class StorageExtTest extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need Location abstract class to which converts batch methods into single instance methods")

  trait TestStorage[F[_]: Sync] extends StorageExt[F]:
    override def createLocations(locations: List[Location.WithOptionalCreatedField]): LocationStream[F] = Stream.empty
    override def getLocations(period: Period, ids: Ids): LocationStream[F] = Stream.empty
    override def updateLocations(locations: List[WithoutCreatedField]): LocationStream[F] = Stream.empty
    override def locationStats(period: Period): LocationStatsStream[F] = Stream.empty
    override def deleteLocations(ids: Ids): F[Int] = Sync[F].delay(0)


  "StorageExt.createLocation" should "fail if batch method stream is empty" in {
    Given("batch method createLocations returns no values")
      val testStorage = new TestStorage[IO] {}
    
    When("createLocation is called")
      val result = testStorage.createLocation(Location("location123", 0, 0, None))
    
    Then("it throws NoSuchElementException")
      a [NoSuchElementException] should be thrownBy result.unsafeRunSync()
  }

  it should "propagate a error if batch method stream raises the error" in {
    Given("batch method createLocations raises a error")
      val error = ServerError.IllegalArgument("Some mistake")
      val testStorage = new TestStorage[IO] {
        override def createLocations(locations: List[WithOptionalCreatedField]): LocationStream[IO] =
          Stream.raiseError(error)
      }
    
    When("createLocation is called")
      val result = testStorage.createLocation(Location("location123", 0, 0, None))
    
    Then("it throws the error")
      a [ServerError] should be thrownBy result.unsafeRunSync()
      result.attempt.unsafeRunSync().left.value shouldBe error
  }

  it should "return a single value if batch method stream contains the single value" in {
    Given("there is a location")
      val source = Location("location123", 0, 0, LocalDateTime.now())

    And("batch method createLocations returns the location")
      val testStorage = new TestStorage[IO] {
        override def createLocations(locations: List[WithOptionalCreatedField]): LocationStream[IO] = 
          Stream.emit(source)
      }
    
    When("createLocation is called")
      val result = testStorage.createLocation(Location("location123", 0, 0, None))
    
    Then("it returns the location")
      result.unsafeRunSync() shouldBe source
  }

  it should "return the first value if batch method stream contains several values" in {
    Given("there is a location list")
      val source = 
        List(
          Location("location123", 0, 0, LocalDateTime.now()),
          Location("location456", 0, 0, LocalDateTime.now()),
          Location("location789", 0, 0, LocalDateTime.now())
        )

    And("batch method createLocations returns the list")
      val testStorage = new TestStorage[IO] {
        override def createLocations(locations: List[WithOptionalCreatedField]): LocationStream[IO] = 
          Stream.emits(source)
      }
    
    When("createLocation is called")
      val result = testStorage.createLocation(Location("location123", 0, 0, None))
    
    Then("it returns the first location in the list")
      result.unsafeRunSync() shouldBe source.head
  }

  "StorageExt.getLocation" should "fail if batch method stream is empty" in {
    Given("batch method getLocations returns no values")
      val testStorage = new TestStorage[IO] {}
    
    When("getLocation is called")
      val result = testStorage.getLocation("location123")
    
    Then("it throws NoSuchElementException")
      a [NoSuchElementException] should be thrownBy result.unsafeRunSync()
  }

  it should "propagate a error if batch method stream raises the error" in {
    Given("batch method getLocations raises a error")
      val error = ServerError.IllegalArgument("Some mistake")
      val testStorage = new TestStorage[IO] {
        override def getLocations(period: Period, ids: Ids): LocationStream[IO] = 
          Stream.raiseError(error)
      }
    
    When("getLocation is called")
      val result = testStorage.getLocation("location123")
    
    Then("it throws the error")
      a [ServerError] should be thrownBy result.unsafeRunSync()
      result.attempt.unsafeRunSync().left.value shouldBe error
  }

  it should "return a single value if batch method stream contains the single value" in {
    Given("there is a location")
      val source = Location("location123", 0, 0, LocalDateTime.now())

    And("batch method getLocations returns the location")
      val testStorage = new TestStorage[IO] {
        override def getLocations(period: Period, ids: Ids): LocationStream[IO] = 
          Stream.emit(source)
      }
    
    When("getLocation is called")
      val result = testStorage.getLocation("location123")
    
    Then("it returns the location")
      result.unsafeRunSync() shouldBe source
  }

  it should "return the first value if batch method stream contains several values" in {
    Given("there is a location list")
      val source = 
        List(
          Location("location123", 0, 0, LocalDateTime.now()),
          Location("location456", 0, 0, LocalDateTime.now()),
          Location("location789", 0, 0, LocalDateTime.now())
        )

    And("batch method getLocations returns the list")
      val testStorage = new TestStorage[IO] {
        override def getLocations(period: Period, ids: Ids): LocationStream[IO] = 
          Stream.emits(source)
      }
    
    When("getLocation is called")
      val result = testStorage.getLocation("location123")
    
    Then("it returns the first location in the list")
      result.unsafeRunSync() shouldBe source.head
  }

  "StorageExt.updateLocation" should "fail if batch method stream is empty" in {
    Given("batch method updateLocations returns no values")
      val testStorage = new TestStorage[IO] {}
    
    When("updateLocation is called")
      val result = testStorage.updateLocation(Location("location123", 0, 0))
    
    Then("it throws NoSuchElementException")
      a [NoSuchElementException] should be thrownBy result.unsafeRunSync()
  }

  it should "propagate a error if batch method stream raises the error" in {
    Given("batch method updateLocations raises a error")
      val error = ServerError.IllegalArgument("Some mistake")
      val testStorage = new TestStorage[IO] {
        override def updateLocations(locations: List[WithoutCreatedField]): LocationStream[IO] =
          Stream.raiseError(error)
      }
    
    When("updateLocation is called")
      val result = testStorage.updateLocation(Location("location123", 0, 0))
    
    Then("it throws the error")
      a [ServerError] should be thrownBy result.unsafeRunSync()
      result.attempt.unsafeRunSync().left.value shouldBe error
  }

  it should "return a single value if batch method stream contains the single value" in {
    Given("there is a location")
      val source = Location("location123", 0, 0, LocalDateTime.now())

    And("batch method updateLocations returns the location")
      val testStorage = new TestStorage[IO] {
        override def updateLocations(locations: List[WithoutCreatedField]): LocationStream[IO] =
          Stream.emit(source)
      }
    
    When("updateLocation is called")
      val result = testStorage.updateLocation(Location("location123", 0, 0))
    
    Then("it returns the location")
      result.unsafeRunSync() shouldBe source
  }

  it should "return the first value if batch method stream contains several values" in {
    Given("there is a location list")
      val source = 
        List(
          Location("location123", 0, 0, LocalDateTime.now()),
          Location("location456", 0, 0, LocalDateTime.now()),
          Location("location789", 0, 0, LocalDateTime.now())
        )

    And("batch method updateLocations returns the list")
      val testStorage = new TestStorage[IO] {
        override def updateLocations(locations: List[WithoutCreatedField]): LocationStream[IO] =
          Stream.emits(source)
      }
    
    When("updateLocation is called")
      val result = testStorage.updateLocation(Location("location123", 0, 0))
    
    Then("it returns the first location in the list")
      result.unsafeRunSync() shouldBe source.head
  }

  "StorageExt.deleteLocation" should "returns the same value as batch method" in {
    Given("batch method deleteLocations returns a value")
      val count = 5
      val testStorage = new TestStorage[IO] {
        override def deleteLocations(ids: Ids): IO[Int] =
          IO.delay(count)
      }
    
    When("deleteLocation is called")
      val result = testStorage.deleteLocation("location123")
    
    Then("it return the value")
      result.unsafeRunSync() shouldBe count
  }

  it should "propagate a error if batch method raises the error" in {
    Given("batch method deleteLocations raises a error")
      val error = ServerError.IllegalArgument("Some mistake")
      val testStorage = new TestStorage[IO] {
        override def deleteLocations(ids: Ids): IO[Int] =
          IO.raiseError(error)
      }
    
    When("deleteLocation is called")
      val result = testStorage.deleteLocation("location123")
    
    Then("it throws the error")
      a [ServerError] should be thrownBy result.unsafeRunSync()
      result.attempt.unsafeRunSync().left.value shouldBe error
  }

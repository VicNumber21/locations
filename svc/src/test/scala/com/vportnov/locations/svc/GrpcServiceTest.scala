package com.vportnov.locations.svc

import org.scalatest.matchers.should.Matchers._
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec

import cats.implicits._
import cats.effect.{ IO, Sync }
import cats.effect.unsafe.implicits.global

import fs2.Stream
import io.grpc._

import java.time.LocalDateTime

import com.vportnov.locations.utils.ServerError
import com.vportnov.locations.model
import com.vportnov.locations.grpc
import com.vportnov.locations.grpc.bindings._
import com.vportnov.locations.model.Location.Ids


class GrpcSeriveceTest extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need to provide implementation of service generated from proto files")

  trait TestStorage[F[_]: Sync] extends model.Storage[F]:
    override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[F] = Stream.empty
    override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[F] = Stream.empty
    override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[F] = Stream.empty
    override def locationStats(period: model.Period): LocationStatsStream[F] = Stream.empty
    override def deleteLocations(ids: model.Location.Ids): F[Int] = Sync[F].delay(0)


  "GrpcService.createLocations" should "return empty stream if storage generates empty stream" in {
    Given("storage generates empty stream")
      val storage = new TestStorage[IO] {}
      val service = new GrpcService(storage)

    When("the service method is called")
      val result = service.createLocations(grpc.Locations(), new Metadata).compile.toList.unsafeRunSync()

    Then("it returns empty stream")
      result shouldBe empty
  }

  it should "return grpc.Location stream if storage generates stream with some locations" in {
    Given("storage generates stream with some locations")
      val initialValue =
        List(
          model.Location("location123", 0, 0, LocalDateTime.now()),
          model.Location("location456", 0, 0, LocalDateTime.now())
        )

      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          Stream.emits(initialValue)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
      val result = service.createLocations(grpc.Locations(), new Metadata).compile.toList.unsafeRunSync()

    Then("it returns LocationReply stream with the locations sent by storage")
      result.map(_.getLocation) shouldBe initialValue.map(_.toMessage)
  }

  it should "propagate error if storage generates stream with failure" in {
    Given("storage generates stream with some locations")
      val initialValue = ServerError.IllegalArgument("Bad argument")

      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          Stream.raiseError(initialValue)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
      note("error log below is expected and belongs to error case test after it")
      noException should be thrownBy service.createLocations(grpc.Locations(), new Metadata)
        .compile.toList.unsafeRunSync()
      val result = service.createLocations(grpc.Locations(), new Metadata)
        .compile.toList.unsafeRunSync().head

    Then("it returns stream with failure from storage")
      result.getServerError shouldBe initialValue.toMessage
  }

  it should "pass empty list into storage method if called with empty list" in {
    Given("empty list is argument of service method")
      val argument = grpc.Locations()

      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          locations shouldBe argument.toLocationsWithOptionalCreatedField
          super.createLocations(locations)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("empty list is got as argument in storage method")
      service.createLocations(argument, new Metadata).compile.toList.unsafeRunSync()
  }

  it should "pass list of locations into storage method if called with non empty list" in {
    Given("non empty list of locations is argument of service method")
      val argument = grpc.Locations().withList(
        List(
          model.Location("location123", 0, 0, LocalDateTime.now()),
          model.Location("location456", 0, 0, LocalDateTime.now())
        ).map(_.toMessage)
      )

      val storage = new TestStorage[IO] {
        override def createLocations(locations: List[model.Location.WithOptionalCreatedField]): LocationStream[IO] =
          locations shouldBe argument.toLocationsWithOptionalCreatedField
          super.createLocations(locations)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("the list of the locations is got as argument in storage method")
      service.createLocations(argument, new Metadata).compile.toList.unsafeRunSync()
  }

  "GrpcService.getLocations" should "return empty stream if storage generates empty stream" in {
    Given("storage generates empty stream")
      val storage = new TestStorage[IO] {}
      val service = new GrpcService(storage)

    When("the service method is called")
      val result = service.getLocations(grpc.Query(), new Metadata).compile.toList.unsafeRunSync()

    Then("it returns empty stream")
      result shouldBe empty
  }

  it should "return grpc.Location stream if storage generates stream with some locations" in {
    Given("storage generates stream with some locations")
      val initialValue =
        List(
          model.Location("location123", 0, 0, LocalDateTime.now()),
          model.Location("location456", 0, 0, LocalDateTime.now())
        )

      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[cats.effect.IO] =
          Stream.emits(initialValue)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
      val result = service.getLocations(grpc.Query(), new Metadata).compile.toList.unsafeRunSync()

    Then("it returns LocationReply stream with the locations sent by storage")
      result.map(_.getLocation) shouldBe initialValue.map(_.toMessage)
  }

  it should "propagate error if storage generates stream with failure" in {
    Given("storage generates stream with some locations")
      val initialValue = ServerError.IllegalArgument("Bad argument")

      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[cats.effect.IO] =
          Stream.raiseError(initialValue)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
      note("error log below is expected and belongs to error case test after it")
      noException should be thrownBy service.getLocations(grpc.Query(), new Metadata)
        .compile.toList.unsafeRunSync()
      val result = service.getLocations(grpc.Query(), new Metadata)
        .compile.toList.unsafeRunSync().head

    Then("it returns stream with failure from storage")
      result.getServerError shouldBe initialValue.toMessage
  }

  it should "pass empty period and empty list of ids into storage method if called with such arguments" in {
    Given("empty period and empty list of ids are arguments of service method")
      val argumentPeriod = model.Period(None, None)
      val argumentIds = List.empty[model.Location.Id]
      val argument = grpc.Query()
        .withPeriod(argumentPeriod.toMessage)
        .withIds(argumentIds.toMessage)

      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[cats.effect.IO] =
          period shouldBe argumentPeriod
          ids shouldBe argumentIds
          super.getLocations(period, ids)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("empty period and empty list of ids are got as arguments in storage method")
      service.getLocations(argument, new Metadata).compile.toList.unsafeRunSync()
  }

  it should "pass non empty period and empty list of ids into storage method if called with such arguments" in {
    Given("non empty period and empty list of ids are arguments of service method")
      val argumentPeriod = model.Period(Some(LocalDateTime.now()), Some(LocalDateTime.now()))
      val argumentIds = List.empty[model.Location.Id]
      val argument = grpc.Query()
        .withPeriod(argumentPeriod.toMessage)
        .withIds(argumentIds.toMessage)

      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[cats.effect.IO] =
          period shouldBe argumentPeriod
          ids shouldBe argumentIds
          super.getLocations(period, ids)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("the period and empty list of ids are got as arguments in storage method")
      service.getLocations(argument, new Metadata).compile.toList.unsafeRunSync()
  }

  it should "pass empty period and non empty list of ids into storage method if called with such arguments" in {
    Given("empty period and non empty list of ids are arguments of service method")
      val argumentPeriod = model.Period(None, None)
      val argumentIds = List("location123", "location456")

      val argument = grpc.Query()
        .withPeriod(argumentPeriod.toMessage)
        .withIds(argumentIds.toMessage)

      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[cats.effect.IO] =
          period shouldBe argumentPeriod
          ids shouldBe argumentIds
          super.getLocations(period, ids)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("empty period and the list of the ids are got as arguments in storage method")
      service.getLocations(argument, new Metadata).compile.toList.unsafeRunSync()
  }

  it should "pass non empty period and non empty list of ids into storage method if called with such arguments" in {
    Given("non empty period and non empty list of ids are arguments of service method")
      val argumentPeriod = model.Period(Some(LocalDateTime.now()), Some(LocalDateTime.now()))
      val argumentIds = List("location123", "location456")

      val argument = grpc.Query()
        .withPeriod(argumentPeriod.toMessage)
        .withIds(argumentIds.toMessage)

      val storage = new TestStorage[IO] {
        override def getLocations(period: model.Period, ids: model.Location.Ids): LocationStream[cats.effect.IO] =
          period shouldBe argumentPeriod
          ids shouldBe argumentIds
          super.getLocations(period, ids)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("the period and the list of the ids are got as arguments in storage method")
      service.getLocations(argument, new Metadata).compile.toList.unsafeRunSync()
  }

  "GrpcService.updateLocations" should "return empty stream if storage generates empty stream" in {
    Given("storage generates empty stream")
      val storage = new TestStorage[IO] {}
      val service = new GrpcService(storage)

    When("the service method is called")
      val result = service.updateLocations(grpc.Locations(), new Metadata).compile.toList.unsafeRunSync()

    Then("it returns empty stream")
      result shouldBe empty
  }

  it should "return grpc.Location stream if storage generates stream with some locations" in {
    Given("storage generates stream with some locations")
      val initialValue =
        List(
          model.Location("location123", 0, 0, LocalDateTime.now()),
          model.Location("location456", 0, 0, LocalDateTime.now())
        )

      val storage = new TestStorage[IO] {
        override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[IO] =
          Stream.emits(initialValue)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
      val result = service.updateLocations(grpc.Locations(), new Metadata).compile.toList.unsafeRunSync()

    Then("it returns LocationReply stream with the locations sent by storage")
      result.map(_.getLocation) shouldBe initialValue.map(_.toMessage)
  }

  it should "propagate error if storage generates stream with failure" in {
    Given("storage generates stream with some locations")
      val initialValue = ServerError.IllegalArgument("Bad argument")

      val storage = new TestStorage[IO] {
        override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[IO] =
          Stream.raiseError(initialValue)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
      note("error log below is expected and belongs to error case test after it")
      noException should be thrownBy service.updateLocations(grpc.Locations(), new Metadata)
        .compile.toList.unsafeRunSync()
      val result = service.updateLocations(grpc.Locations(), new Metadata)
        .compile.toList.unsafeRunSync().head

    Then("it returns stream with failure from storage")
      result.getServerError shouldBe initialValue.toMessage
  }

  it should "pass empty list into storage method if called with empty list" in {
    Given("empty list is argument of service method")
      val argument = grpc.Locations()

      val storage = new TestStorage[IO] {
        override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[IO] =
          locations shouldBe argument.toLocationsWithoutCreatedField
          super.updateLocations(locations)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("empty list is got as argument in storage method")
      service.updateLocations(argument, new Metadata).compile.toList.unsafeRunSync()
  }

  it should "pass list of locations into storage method if called with non empty list" in {
    Given("non empty list of locations is argument of service method")
      val argument = grpc.Locations().withList(
        List(
          model.Location("location123", 0, 0),
          model.Location("location456", 0, 0)
        ).map(_.toMessage)
      )

      val storage = new TestStorage[IO] {
        override def updateLocations(locations: List[model.Location.WithoutCreatedField]): LocationStream[IO] =
          locations shouldBe argument.toLocationsWithoutCreatedField
          super.updateLocations(locations)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("the list of the locations is got as argument in storage method")
      service.updateLocations(argument, new Metadata).compile.toList.unsafeRunSync()
  }

  "GrpcService.deleteLocations" should "return 0 if storage returns 0" in {
    Given("storage returns 0")
      val storage = new TestStorage[IO] {}
      val service = new GrpcService(storage)

    When("the service method is called")
      val result = service.deleteLocations(grpc.Ids(), new Metadata).unsafeRunSync()

    Then("it returns CountReply with 0")
      result.getCount.value shouldBe 0
  }

  it should "return positive value if storage returns positive value" in {
    Given("storage returns positive value")
      val initialValue = 5

      val storage = new TestStorage[IO] {
        override def deleteLocations(ids: Ids): IO[Int] = IO(initialValue)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
      val result = service.deleteLocations(grpc.Ids(), new Metadata).unsafeRunSync()

    Then("it returns CountReply with positive value returned by storage")
      result.getCount.value shouldBe initialValue
  }

  it should "propagate error if storage generates reply with failure" in {
    Given("storage generates reply with failure")
      val initialValue = ServerError.IllegalArgument("Bad argument")

      val storage = new TestStorage[IO] {
        override def deleteLocations(ids: Ids): IO[Int] = IO.raiseError(initialValue)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
      note("error log below is expected and belongs to error case test after it")
      noException should be thrownBy service.deleteLocations(grpc.Ids(), new Metadata).unsafeRunSync()
      val result = service.deleteLocations(grpc.Ids(), new Metadata).unsafeRunSync()

    Then("it returns CountReply with failure from storage")
      result.getServerError shouldBe initialValue.toMessage
  }

  it should "pass empty list of ids into storage method if called with such argument" in {
    Given("empty list of ids is argument of service method")
      val argument = grpc.Ids()

      val storage = new TestStorage[IO] {
        override def deleteLocations(ids: Ids): IO[Int] =
          ids shouldBe argument.toModel
          super.deleteLocations(ids)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("empty list of ids is got as argument in storage method")
      service.deleteLocations(argument, new Metadata).unsafeRunSync()
  }

  it should "pass non empty list of ids into storage method if called with such argument" in {
    Given("non empty list of ids is argument of service method")
      val argumentIds = List("location123", "location456")
      val argument = argumentIds.toMessage

      val storage = new TestStorage[IO] {
        override def deleteLocations(ids: Ids): IO[Int] =
          ids shouldBe argumentIds
          super.deleteLocations(ids)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("the list of the ids is got as argument in storage method")
      service.deleteLocations(argument, new Metadata).unsafeRunSync()
  }

  "GrpcService.locationStats" should "return empty stream if storage generates empty stream" in {
    Given("storage generates empty stream")
      val storage = new TestStorage[IO] {}
      val service = new GrpcService(storage)

    When("the service method is called")
      val result = service.locationStats(grpc.Period(), new Metadata).compile.toList.unsafeRunSync()

    Then("it returns empty stream")
      result shouldBe empty
  }

  it should "return grpc.Location stream if storage generates stream with some locations" in {
    Given("storage generates stream with some locations")
      val initialValue =
        List(
          model.Location.Stats(LocalDateTime.now(), 5),
          model.Location.Stats(LocalDateTime.now(), 2)
        )

      val storage = new TestStorage[IO] {
        override def locationStats(period: model.Period): LocationStatsStream[IO] =
          Stream.emits(initialValue)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
      val result = service.locationStats(grpc.Period(), new Metadata).compile.toList.unsafeRunSync()

    Then("it returns LocationStatsReply stream with the locations sent by storage")
      result.map(_.getLocationStats) shouldBe initialValue.map(_.toMessage)
  }

  it should "propagate error if storage generates stream with failure" in {
    Given("storage generates stream with some locations")
      val initialValue = ServerError.IllegalArgument("Bad argument")

      val storage = new TestStorage[IO] {
        override def locationStats(period: model.Period): LocationStatsStream[IO] =
          Stream.raiseError(initialValue)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
      note("error log below is expected and belongs to error case test after it")
      noException should be thrownBy service.locationStats(grpc.Period(), new Metadata)
        .compile.toList.unsafeRunSync()
      val result = service.locationStats(grpc.Period(), new Metadata)
        .compile.toList.unsafeRunSync().head

    Then("it returns stream with failure from storage")
      result.getServerError shouldBe initialValue.toMessage
  }

  it should "pass empty period into storage method if called with such argument" in {
    Given("empty period is arguments of service method")
      val argumentPeriod = model.Period(None, None)
      val argument = argumentPeriod.toMessage

      val storage = new TestStorage[IO] {
        override def locationStats(period: model.Period): LocationStatsStream[IO] =
          period shouldBe argumentPeriod
          super.locationStats(period)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("empty period is got as argument in storage method")
      service.locationStats(argument, new Metadata).compile.toList.unsafeRunSync()
  }

  it should "pass non empty period storage method if called with such argument" in {
    Given("non empty period is argument of service method")
      val argumentPeriod = model.Period(Some(LocalDateTime.now()), Some(LocalDateTime.now()))
      val argument = argumentPeriod.toMessage

      val storage = new TestStorage[IO] {
        override def locationStats(period: model.Period): LocationStatsStream[IO] =
          period shouldBe argumentPeriod
          super.locationStats(period)
      }

      val service = new GrpcService(storage)

    When("the service method is called")
    Then("the period is got as argument in storage method")
      service.locationStats(argument, new Metadata).compile.toList.unsafeRunSync()
  }

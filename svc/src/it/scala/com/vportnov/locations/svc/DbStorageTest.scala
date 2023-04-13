package com.vportnov.locations.svc

import org.scalatest.{ DoNotDiscover, BeforeAndAfterEach }
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers._

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor

import java.time.{ LocalDateTime, ZoneOffset }

import com.vportnov.locations.model
import com.vportnov.locations.utils.ServerError


@DoNotDiscover
class DbStorageTest extends AnyDbSpec with BeforeAndAfterEach:
  info("As a developer I need to implement an object which run requests to database")

  override protected def beforeEach(): Unit =
    cleanDb()

  override protected def afterEach(): Unit = 
    cleanDb()

  "DbStorage.createLocations" should "generate stream with IllegalArgument ServerError if empty list is given as argument" in {
    Given("argument is an empty list")
      val argument = List.empty[model.Location.WithOptionalCreatedField]

    When("the method is called with such argument")
      a [ServerError] should be thrownBy dbStorage.createLocations(argument).compile.toList.unsafeRunSync()
      val result = dbStorage.createLocations(argument)
        .attempt
        .compile.toList.unsafeRunSync()
        .head.left.value.asInstanceOf[ServerError]
    
    Then("result is IllegalArgument ServerError")
      result.kind shouldBe ServerError.Kind.IllegalArgument
  }

  it should "generate stream of created locations if non empty list is given as argument" in {
    Given("argument is list of locations")
      val argument =
        List(
          model.Location.WithOptionalCreatedField("location123", 2.345, -7.567, Some(LocalDateTime.now())),
          model.Location.WithOptionalCreatedField("location456", -79, 83, Some(LocalDateTime.now))
        )

    When("the method is called with such argument")
      val result = dbStorage.createLocations(argument).compile.toList.unsafeRunSync()

    Then("result consists of created locations")
      result.toSortedTuple4 shouldBe argument.toSortedTuple4
    
    And("created locations exist in database")
      sortedEntriesFromDb shouldBe argument.toSortedTuple4
  }

  it should "generate stream of created locations with automatically created field if non empty list of location without created field is given as argument" in {
    Given("argument is list of locations without created field")
      val argument =
        List(
          model.Location.WithOptionalCreatedField("location123", 1, 1, None),
          model.Location.WithOptionalCreatedField("location456", 2, 2, None),
          model.Location.WithOptionalCreatedField("location789", 3, 3, None)
        )

    When("the method is called with such argument")
      val callTimestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
      val result = dbStorage.createLocations(argument).compile.toList.unsafeRunSync()

    Then("result consists of created locations")
      result.toSortedTuple3 shouldBe argument.toSortedTuple3
    
    And("their created fields are about call timestamp")
      all (result.map(_.created.toEpochSecond(ZoneOffset.UTC))) should be (callTimestamp +- 1)
    
    And("created locations exist in database")
      val dbEntries = sortedEntriesFromDb
      dbEntries.map(_.take(3)) shouldBe argument.toSortedTuple3

    And("their created fields are about call timestamp")
      all (dbEntries.map(_._4.toEpochSecond(ZoneOffset.UTC))) should be (callTimestamp +- 1)
  }

  it should "creates locations with unique ids" in {
    Given("argument is list of locations with non uqnique ids")
      val argument =
        List(
          model.Location.WithOptionalCreatedField("location123", 1, 1, Some(LocalDateTime.now())),
          model.Location.WithOptionalCreatedField("location123", 2, 2, Some(LocalDateTime.now())),
          model.Location.WithOptionalCreatedField("location123", 3, 3, Some(LocalDateTime.now()))
        )

    When("the method is called with such argument")
      val result = dbStorage.createLocations(argument).compile.toList.unsafeRunSync()

    Then("result contains just first location instance")
      result.toSortedTuple4 shouldBe argument.take(1).toSortedTuple4

    And("the created location exists in database")
      sortedEntriesFromDb shouldBe argument.take(1).toSortedTuple4
  }

  it should "creates just locations with ids which are not used yet" in {
    Given("there are several locations in database")
      val existingEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("location456", -1, -1, LocalDateTime.now()),
          ("location789", -2, -2, LocalDateTime.now())
        )
          .sortBy(_._1)

      val existingIdList = existingEntries.map(_._1)
      insertIntoDb(existingEntries)

    And("argument is list of locations where some of ids are the same as existing ones with different values")
      val argument =
        List(
          model.Location.WithOptionalCreatedField("location123", 1, 1, Some(LocalDateTime.now())),
          model.Location.WithOptionalCreatedField("location456", 2, 2, Some(LocalDateTime.now())),
          model.Location.WithOptionalCreatedField("location789", 3, 3, Some(LocalDateTime.now())),
          model.Location.WithOptionalCreatedField("oneMoreLocation", 3, 3, Some(LocalDateTime.now()))
        )
      
      existingEntries should not be argument.filter(l => existingIdList.contains(l.id)).toSortedTuple4

    When("the method is called with such argument")
      val result = dbStorage.createLocations(argument).compile.toList.unsafeRunSync()

    Then("result contains just locations with new ids")
      val expectedEntries = argument.filterNot(l => existingIdList.contains(l.id)).toSortedTuple4
      result.toSortedTuple4 shouldBe expectedEntries

    And("new locations are added to database and old locations are not changed")
      sortedEntriesFromDb shouldBe (existingEntries ++ expectedEntries).sortBy(_._1)
  }

  "DbStorage.getLocations" should "generate stream with IllegalArgument ServerError if both period and list of ids are given as arguments" in {
    Given("arguments are non empty preriod and non empty list of ids")
      val argumentPeriod = model.Period(Some(LocalDateTime.now()), Some(LocalDateTime.now()))
      val argumentIds = List("location123", "location456")

    When("the method is called with such arguments")
      a [ServerError] should be thrownBy dbStorage.getLocations(argumentPeriod, argumentIds).compile.toList.unsafeRunSync()
      val result = dbStorage.getLocations(argumentPeriod, argumentIds)
        .attempt
        .compile.toList.unsafeRunSync()
        .head.left.value.asInstanceOf[ServerError]
    
    Then("result is IllegalArgument ServerError")
      result.kind shouldBe ServerError.Kind.IllegalArgument
  }

  it should "return empty stream if no entries in database and no filter given" in {
    Given("database is empty")

    And("arguments are empty period and empty list of ids")
      val argumentPeriod = model.Period(None, None)
      val argumentIds = List.empty[model.Location.Id]

    When("the method is called with such arguments")
      val result = dbStorage.getLocations(argumentPeriod, argumentIds).compile.toList.unsafeRunSync()

    Then("empty stream of locations is returned")
      result shouldBe List.empty[model.Location.WithCreatedField]
  }

  it should "return empty stream if no entries in database and period is not empty" in {
    Given("database is empty")

    And("arguments are non empty period and empty list of ids")
      val argumentPeriod = model.Period(Some(LocalDateTime.now()), None)
      val argumentIds = List.empty[model.Location.Id]

    When("the method is called with such arguments")
      val result = dbStorage.getLocations(argumentPeriod, argumentIds).compile.toList.unsafeRunSync()

    Then("empty stream of locations is returned")
      result shouldBe List.empty[model.Location.WithCreatedField]
  }

  it should "return empty stream if no entries in database and non empty list of ids is given" in {
    Given("database is empty")

    And("arguments are empty period and non empty list of ids")
      val argumentPeriod = model.Period(None, None)
      val argumentIds = List("location123", "location456")

    When("the method is called with such arguments")
      val result = dbStorage.getLocations(argumentPeriod, argumentIds).compile.toList.unsafeRunSync()

    Then("empty stream of locations is returned")
      result shouldBe List.empty[model.Location.WithCreatedField]
  }

  it should "return all entries if database is non empty and no filter given" in {
    Given("database is not empty")
      val databaseEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("past", -2, -2, LocalDateTime.now().minusMonths(1)),
          ("yesterday", -1, -1, LocalDateTime.now().minusDays(1)),
          ("today01", 0, 0, LocalDateTime.now()),
          ("today02", 0, 0, LocalDateTime.now()),
          ("tomorrow", 1, 1, LocalDateTime.now().plusDays(1)),
          ("future", 2, 2, LocalDateTime.now().plusMonths(1))
        )
          .sortBy(_._1)
        
      insertIntoDb(databaseEntries)

    And("arguments are empty period and empty list of ids")
      val argumentPeriod = model.Period(None, None)
      val argumentIds = List.empty[model.Location.Id]

    When("the method is called with such arguments")
      val result = dbStorage.getLocations(argumentPeriod, argumentIds).compile.toList.unsafeRunSync()

    Then("all database entries are returned")
      result.toSortedTuple4 shouldBe databaseEntries
  }

  it should "return empty stream if database is non empty but list of unexisting ids is given" in {
    Given("database is not empty")
      val databaseEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("past", -2, -2, LocalDateTime.now().minusMonths(1)),
          ("yesterday", -1, -1, LocalDateTime.now().minusDays(1)),
          ("today01", 0, 0, LocalDateTime.now()),
          ("today02", 0, 0, LocalDateTime.now()),
          ("tomorrow", 1, 1, LocalDateTime.now().plusDays(1)),
          ("future", 2, 2, LocalDateTime.now().plusMonths(1))
        )
          .sortBy(_._1)
        
      insertIntoDb(databaseEntries)

    And("arguments are empty period and list of unexisting ids")
      val argumentPeriod = model.Period(None, None)
      val argumentIds = List("IDoNotExist", "MeToo")

    When("the method is called with such arguments")
      val result = dbStorage.getLocations(argumentPeriod, argumentIds).compile.toList.unsafeRunSync()

    Then("empty stream of locations is returned")
      result shouldBe List.empty[model.Location.WithCreatedField]
  }

  it should "return stream of requested entries if database is non empty and list of some existing ids is given" in {
    Given("database is not empty")
      val databaseEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("past", -2, -2, LocalDateTime.now().minusMonths(1)),
          ("yesterday", -1, -1, LocalDateTime.now().minusDays(1)),
          ("today01", 0, 0, LocalDateTime.now()),
          ("today02", 0, 0, LocalDateTime.now()),
          ("tomorrow", 1, 1, LocalDateTime.now().plusDays(1)),
          ("future", 2, 2, LocalDateTime.now().plusMonths(1))
        )
          .sortBy(_._1)
        
      insertIntoDb(databaseEntries)

    And("arguments are empty period and list of some existing ids")
      val argumentPeriod = model.Period(None, None)
      val argumentIds = List("today01", "future")

    When("the method is called with such arguments")
      val result = dbStorage.getLocations(argumentPeriod, argumentIds).compile.toList.unsafeRunSync()

    Then("requested database entries are returned")
      result should have length argumentIds.length
      result.toSortedTuple4 shouldBe databaseEntries.filter(l => argumentIds.contains(l._1))
  }

  it should "return stream of existing entries if database is non empty but some ids from given list do not exist" in {
    Given("database is not empty")
      val databaseEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("past", -2, -2, LocalDateTime.now().minusMonths(1)),
          ("yesterday", -1, -1, LocalDateTime.now().minusDays(1)),
          ("today01", 0, 0, LocalDateTime.now()),
          ("today02", 0, 0, LocalDateTime.now()),
          ("tomorrow", 1, 1, LocalDateTime.now().plusDays(1)),
          ("future", 2, 2, LocalDateTime.now().plusMonths(1))
        )
          .sortBy(_._1)
        
      insertIntoDb(databaseEntries)

    And("arguments are empty period and list of some existing ids")
      val argumentPeriod = model.Period(None, None)
      val argumentIds = List("past", "ImNotThere", "today02", "future", "CannotBeFound")

    When("the method is called with such arguments")
      val result = dbStorage.getLocations(argumentPeriod, argumentIds).compile.toList.unsafeRunSync()

    Then("requested database entries are returned")
      val expectedEntries = databaseEntries.filter(l => argumentIds.contains(l._1))
      expectedEntries.length should be > 0
      expectedEntries.length should be < argumentIds.length
      result.toSortedTuple4 shouldBe expectedEntries 
  }

  it should "return stream with all entries newer or equal to period 'from' if database is non empty" in {
    Given("database is not empty")
      val now = LocalDateTime.now()
      val todayMidnight = now.toLocalDate().atStartOfDay()
      val todayMidday = now.toLocalDate().atTime(12, 0)
      val entryIds = List("past", "yesterday", "today01", "today02", "todayMidnight", "tomorrowMidnight", "tomorrow", "future")

      val databaseEntries = entryIds
        .map(id => id match
          case "past" => (id, now.minusMonths(1))
          case "yesterday" => (id, now.minusDays(1))
          case "today01" => (id, todayMidday.minusHours(3))
          case "today02" => (id, todayMidday.plusHours(5))
          case "todayMidnight" => (id, todayMidnight)
          case "tomorrowMidnight" => (id, todayMidnight.plusDays(1))
          case "tomorrow" => (id, now.plusDays(1))
          case "future" => (id, now.plusMonths(1))
        )
        .map(l => (l._1, BigDecimal(0), BigDecimal(0), l._2))
        .sortBy(_._1)
        
      insertIntoDb(databaseEntries)

    And("arguments are period with 'from' and empty list of ids")
      val argumentPeriod = model.Period(Some(todayMidday), None)
      val argumentIds = List.empty[model.Location.Id]

    When("the method is called with such arguments")
      val result = dbStorage.getLocations(argumentPeriod, argumentIds).compile.toList.unsafeRunSync()

    Then("database entries newer or equal to period 'from' (edge date included) are returned")
      val expectedIds = entryIds.drop(2)
      result.toSortedTuple4 shouldBe databaseEntries.filter(l => expectedIds.contains(l._1))
  }

  it should "return stream with all entries older or equal to period 'to' if database is non empty" in {
    Given("database is not empty")
      val now = LocalDateTime.now()
      val todayMidnight = now.toLocalDate().atStartOfDay()
      val todayMidday = now.toLocalDate().atTime(12, 0)
      val entryIds = List("past", "yesterday", "today01", "today02", "todayMidnight", "tomorrowMidnight", "tomorrow", "future")

      val databaseEntries = entryIds
        .map(id => id match
          case "past" => (id, now.minusMonths(1))
          case "yesterday" => (id, now.minusDays(1))
          case "today01" => (id, todayMidday.minusHours(3))
          case "today02" => (id, todayMidday.plusHours(5))
          case "todayMidnight" => (id, todayMidnight)
          case "tomorrowMidnight" => (id, todayMidnight.plusDays(1))
          case "tomorrow" => (id, now.plusDays(1))
          case "future" => (id, now.plusMonths(1))
        )
        .map(l => (l._1, BigDecimal(0), BigDecimal(0), l._2))
        .sortBy(_._1)
        
      insertIntoDb(databaseEntries)

    And("arguments are period with 'to' and empty list of ids")
      val argumentPeriod = model.Period(None, Some(todayMidday))
      val argumentIds = List.empty[model.Location.Id]

    When("the method is called with such arguments")
      val result = dbStorage.getLocations(argumentPeriod, argumentIds).compile.toList.unsafeRunSync()

    Then("database entries older or equal to period 'to' (edge date included) are returned")
      val expectedIds = entryIds.take(5)
      result.toSortedTuple4 shouldBe databaseEntries.filter(l => expectedIds.contains(l._1))
  }

  it should "return stream with all entries between period 'from' and 'to' (inclusively) if database is non empty" in {
    Given("database is not empty")
      val now = LocalDateTime.now()
      val todayMidnight = now.toLocalDate().atStartOfDay()
      val todayMidday = now.toLocalDate().atTime(12, 0)
      val entryIds = List("past", "yesterday", "today01", "today02", "todayMidnight", "tomorrowMidnight", "tomorrow", "future")

      val databaseEntries = entryIds
        .map(id => id match
          case "past" => (id, now.minusMonths(1))
          case "yesterday" => (id, now.minusDays(1))
          case "today01" => (id, todayMidday.minusHours(3))
          case "today02" => (id, todayMidday.plusHours(5))
          case "todayMidnight" => (id, todayMidnight)
          case "tomorrowMidnight" => (id, todayMidnight.plusDays(1))
          case "tomorrow" => (id, now.plusDays(1))
          case "future" => (id, now.plusMonths(1))
        )
        .map(l => (l._1, BigDecimal(0), BigDecimal(0), l._2))
        .sortBy(_._1)
        
      insertIntoDb(databaseEntries)

    And("arguments are period with 'from' and 'to' and empty list of ids")
      val argumentPeriod = model.Period(Some(todayMidday), Some(todayMidday.plusDays(1)))
      val argumentIds = List.empty[model.Location.Id]

    When("the method is called with such arguments")
      val result = dbStorage.getLocations(argumentPeriod, argumentIds).compile.toList.unsafeRunSync()

    Then("database entries newer or equal to period 'from' and older or equal to period 'to' (edge dates included) are returned")
      val expectedIds = entryIds.drop(2).take(5)
      result.toSortedTuple4 shouldBe databaseEntries.filter(l => expectedIds.contains(l._1))
  }

  "DbStorage.updateLocations" should "generate stream with IllegalArgument ServerError if empty list is given as argument" in {
    Given("argument is an empty list")
      val argument = List.empty[model.Location.WithoutCreatedField]

    When("the method is called with such argument")
      a [ServerError] should be thrownBy dbStorage.updateLocations(argument).compile.toList.unsafeRunSync()
      val result = dbStorage.updateLocations(argument)
        .attempt
        .compile.toList.unsafeRunSync()
        .head.left.value.asInstanceOf[ServerError]
    
    Then("result is IllegalArgument ServerError")
      result.kind shouldBe ServerError.Kind.IllegalArgument
  }

  it should "return empty stream if database is empty and non empty list is given as argument" in {
    Given("argument is non empty list")
      val argument =
        List(
          model.Location.WithoutCreatedField("location123", 1, 1),
          model.Location.WithoutCreatedField("location456", 2, 2),
          model.Location.WithoutCreatedField("location789", 3, 3)
        )

    When("the method is called with such argument")
      val result = dbStorage.updateLocations(argument).compile.toList.unsafeRunSync()

    Then("returned result is empty stream")
      result shouldBe List.empty[model.Location.WithCreatedField]
  }

  it should "update existing locations if they are given as argument" in {
    Given("there are several locations in database")
      val existingEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("location123", -1, -1, LocalDateTime.now()),
          ("location456", -2, -2, LocalDateTime.now()),
          ("location789", -3, -3, LocalDateTime.now())
        )
          .sortBy(_._1)

      val existingIdList = existingEntries.map(_._1)
      insertIntoDb(existingEntries)

    And("argument is list of locations where some of ids are the same as existing ones with different values")
      val argument =
        List(
          model.Location.WithoutCreatedField("location123", 1, 1),
          model.Location.WithoutCreatedField("location789", 3, 3)
        )

    When("the method is called with such argument")
      val result = dbStorage.updateLocations(argument).compile.toList.unsafeRunSync()

    Then("result contains just locations with existing ids")
      val expectedEntries = argument.filter(l => existingIdList.contains(l.id)).toSortedTuple3
      result.toSortedTuple3 shouldBe expectedEntries

    And("found locations are updated in database and other locations are not changed")
      val updatedIdList = argument.map(_.id).filter(id => existingIdList.contains(id))
      val notUpdatedEntries = existingEntries.filterNot(l => updatedIdList.contains(l._1)).map(_.take(3))
      sortedEntriesFromDb.map(_.take(3)) shouldBe (notUpdatedEntries ++ expectedEntries).sortBy(_._1)
  }

  it should "update existing locations and ignore unexisting if they are given as argument" in {
    Given("there are several locations in database")
      val existingEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("location123", -1, -1, LocalDateTime.now()),
          ("location456", -2, -2, LocalDateTime.now()),
          ("location789", -3, -3, LocalDateTime.now())
        )
          .sortBy(_._1)

      val existingIdList = existingEntries.map(_._1)
      insertIntoDb(existingEntries)

    And("argument is list of locations where some of ids are the same as existing ones with different values")
      val argument =
        List(
          model.Location.WithoutCreatedField("location123", 1, 1),
          model.Location.WithoutCreatedField("location789", 3, 3),
          model.Location.WithoutCreatedField("oneMoreLocation", 4, 4)
        )
      
      existingEntries.map(_.take(3)) should not be argument.filter(l => existingIdList.contains(l.id)).toSortedTuple3

    When("the method is called with such argument")
      val result = dbStorage.updateLocations(argument).compile.toList.unsafeRunSync()

    Then("result contains just locations with existing ids")
      val expectedEntries = argument.filter(l => existingIdList.contains(l.id)).toSortedTuple3
      result.toSortedTuple3 shouldBe expectedEntries

    And("found locations are updated in database and other locations are not changed")
      val updatedIdList = argument.map(_.id).filter(id => existingIdList.contains(id))
      val notUpdatedEntries = existingEntries.filterNot(l => updatedIdList.contains(l._1)).map(_.take(3))
      sortedEntriesFromDb.map(_.take(3)) shouldBe (notUpdatedEntries ++ expectedEntries).sortBy(_._1)
  }

  it should "not update created field" in {
    Given("there is a location in database which is going to be updated")
      val existingEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("location123", -1, -1, LocalDateTime.now())
        )

      insertIntoDb(existingEntries)

    And("argument contains only location with existing id")
      val argument =
        List(
          model.Location.WithoutCreatedField("location123", 3, 3)
        )
      
      existingEntries.map(_._1) shouldBe argument.map(_.id)
      existingEntries.map(_.take(3)) should not be argument.toSortedTuple3

    When("the method is called with such argument")
      val result = dbStorage.updateLocations(argument).compile.toList.unsafeRunSync()

    Then("result contains updated location but created field is not updated")
      result.toSortedTuple3 shouldBe argument.toSortedTuple3
      result.map(_.created) shouldBe existingEntries.map(_._4)

    And("the updated location exists in database but created field is not changed")
      val dbEntries = sortedEntriesFromDb
      dbEntries.map(_.take(3)) shouldBe argument.toSortedTuple3
      dbEntries.map(_._4) shouldBe existingEntries.map(_._4)
  }

  it should "apply updates in order" in {
    Given("there is a location in database which is going to be updated")
      val existingEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("location123", -1, -1, LocalDateTime.now())
        )

      insertIntoDb(existingEntries)

    And("argument is list of locations with same ids")
      val argument =
        List(
          model.Location.WithoutCreatedField("location123", 1, 1),
          model.Location.WithoutCreatedField("location123", 2, 2),
          model.Location.WithoutCreatedField("location123", 3, 3)
        )

    When("the method is called with such argument")
      val result = dbStorage.updateLocations(argument).compile.toList.unsafeRunSync()

    Then("result contains just last location instance from argument")
      result.toSortedTuple3 shouldBe argument.drop(2).toSortedTuple3

    And("the updated location exists in database")
      sortedEntriesFromDb.map(_.take(3)) shouldBe argument.drop(2).toSortedTuple3
  }

  "DbStorage.deleteLocations" should "raise IllegalArgument ServerError if no ids given as argument" in {
    Given("argument is an empty list")
      val argument = List.empty[model.Location.Id]

    When("the method is called with such argument")
      a [ServerError] should be thrownBy dbStorage.deleteLocations(argument).unsafeRunSync()
      val result = dbStorage.deleteLocations(argument)
        .attempt.unsafeRunSync()
        .left.value.asInstanceOf[ServerError]
    
    Then("result is IllegalArgument ServerError")
      result.kind shouldBe ServerError.Kind.IllegalArgument
  }

  it should "return 0 if no entries in database and some ids given" in {
    Given("database is empty")

    And("argument is non empty list")
      val argument = List("location123", "location456")

    When("the method is called with such argument")
      val result = dbStorage.deleteLocations(argument).unsafeRunSync()
    
    Then("result is 0")
      result shouldBe 0
  }

  it should "return 0 if database is not empty but unexisting ids given" in {
    Given("there are several locations in database")
      val existingEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("location123", 1, -1, LocalDateTime.now()),
          ("location456", 2, -2, LocalDateTime.now()),
          ("location789", 3, -3, LocalDateTime.now())
        )
          .sortBy(_._1)

      insertIntoDb(existingEntries)

    And("argument is non empty list")
      val argument = List("MeNotHere", "MeToo")

    When("the method is called with such argument")
      val result = dbStorage.deleteLocations(argument).unsafeRunSync()
    
    Then("result is 0")
      result shouldBe 0
    
    And("all entries kept in database")
      sortedEntriesFromDb shouldBe existingEntries
  }

  it should "remove all requested locations and return its count if database is not empty and all given ids exist" in {
    Given("there are several locations in database")
      val existingEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("location123", 1, -1, LocalDateTime.now()),
          ("location456", 2, -2, LocalDateTime.now()),
          ("location789", 3, -3, LocalDateTime.now())
        )
          .sortBy(_._1)

      insertIntoDb(existingEntries)

    And("argument is non empty list")
      val argument = List("location456", "location123")

    When("the method is called with such argument")
      val result = dbStorage.deleteLocations(argument).unsafeRunSync()
    
    Then("result is count of given ids")
      result shouldBe argument.length
    
    And("requested entries removed and other entries kept in database")
      sortedEntriesFromDb shouldBe existingEntries.filterNot(l => argument.contains(l._1))
  }

  it should "remove found locations and return its count if database is not empty and just some existing ids given" in {
    Given("there are several locations in database")
      val existingEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("location123", 1, -1, LocalDateTime.now()),
          ("location456", 2, -2, LocalDateTime.now()),
          ("location789", 3, -3, LocalDateTime.now())
        )
          .sortBy(_._1)

      insertIntoDb(existingEntries)
      val existingIds = existingEntries.map(_._1)

    And("argument is non empty list")
      val argument = List("location456", "meNotHere")

    When("the method is called with such argument")
      val result = dbStorage.deleteLocations(argument).unsafeRunSync()
    
    Then("result is count of removed entries")
      val expectedCount = argument.filter(id => existingIds.contains(id)).length
      result shouldBe expectedCount
    
    And("requested entries removed and other entries kept in database")
      sortedEntriesFromDb shouldBe existingEntries.filterNot(l => argument.contains(l._1))
  }

  it should "remove found locations and return its count if database is not empty and duplicated ids given" in {
    Given("there are several locations in database")
      val existingEntries =
        List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]](
          ("location123", 1, -1, LocalDateTime.now()),
          ("location456", 2, -2, LocalDateTime.now()),
          ("location789", 3, -3, LocalDateTime.now())
        )
          .sortBy(_._1)

      insertIntoDb(existingEntries)

    And("argument is non empty list with duplicates")
      val argument = List("location456", "location456")

    When("the method is called with such argument")
      val result = dbStorage.deleteLocations(argument).unsafeRunSync()
    
    Then("result is count of removed entries")
      result shouldBe argument.distinct.length
    
    And("requested entries removed and other entries kept in database")
      sortedEntriesFromDb shouldBe existingEntries.filterNot(l => argument.contains(l._1))
  }

  "DbStorage.locationStats" should "return empty statistics if no entries in database and no filter given" in {
    Given("database is empty")

    And("argument is empty period ")
      val argumentPeriod = model.Period(None, None)

    When("the method is called with such argument")
      val result = dbStorage.locationStats(argumentPeriod).compile.toList.unsafeRunSync()

    Then("empty statistics is returned")
      result shouldBe List.empty[model.Location.Stats]
  }

  it should "return empty statistics if no entries in database and period is not empty" in {
    Given("database is empty")

    And("argument is non empty period ")
      val argumentPeriod = model.Period(Some(LocalDateTime.now()), None)

    When("the method is called with such argument")
      val result = dbStorage.locationStats(argumentPeriod).compile.toList.unsafeRunSync()

    Then("empty statistics is returned")
      result shouldBe List.empty[model.Location.Stats]
  }

  it should "count all entries if database is non empty and no filter given" in {
    Given("database is not empty")
      val now = LocalDateTime.now()

      insertIntoDb(
        List(
          ("past", -2, -2, now.minusMonths(1)),
          ("yesterday", -1, -1, now.minusDays(1)),
          ("today01", 0, 0, now),
          ("today02", 0, 0, now),
          ("tomorrow", 1, 1, now.plusDays(1)),
          ("future", 2, 2, now.plusMonths(1))
        )
      )

    And("argument is empty period")
      val argumentPeriod = model.Period(None, None)

    When("the method is called with such argument")
      val result = dbStorage.locationStats(argumentPeriod).compile.toList.unsafeRunSync()

    Then("all database entries are counted in statistics")
      val expectedStatistics = List(
        model.Location.Stats(now.toLocalDate().minusMonths(1).atStartOfDay(), 1),
        model.Location.Stats(now.toLocalDate().minusDays(1).atStartOfDay(), 1),
        model.Location.Stats(now.toLocalDate().atStartOfDay(), 2),
        model.Location.Stats(now.toLocalDate().plusDays(1).atStartOfDay(), 1),
        model.Location.Stats(now.toLocalDate().plusMonths(1).atStartOfDay(), 1)
      )

      result shouldBe expectedStatistics
  }

  it should "count all entries newer or equal to period 'from' if database is non empty" in {
    Given("database is not empty")
      val now = LocalDateTime.now()
      val todayMidnight = now.toLocalDate().atStartOfDay
      val todayMidday = now.toLocalDate().atTime(12, 0)

      insertIntoDb(
        List(
          ("past", 0, 0, now.minusMonths(1)),
          ("yesterday", 0, 0, now.minusDays(1)),
          ("today01", 0, 0, todayMidday.minusHours(3)),
          ("today02", 0, 0, todayMidday.plusHours(5)),
          ("todayMidnight", 0, 0, todayMidnight),
          ("tomorrowMidnight", 0, 0, todayMidnight.plusDays(1)),
          ("tomorrow", 0, 0, now.plusDays(1)),
          ("future", 0, 0, now.plusMonths(1))
        )
      )

    And("argument is period with 'from'")
      val argumentPeriod = model.Period(Some(todayMidday), None)

    When("the method is called with such argument")
      val result = dbStorage.locationStats(argumentPeriod).compile.toList.unsafeRunSync()

    Then("database entries newer or equal to period 'from' (edge date included) are counted in statistics")
      val expectedStatistics = List(
        model.Location.Stats(todayMidnight, 3),
        model.Location.Stats(now.toLocalDate().plusDays(1).atStartOfDay(), 2),
        model.Location.Stats(now.toLocalDate().plusMonths(1).atStartOfDay(), 1)
      )

      result shouldBe expectedStatistics
  }

  it should "count all entries older or equal to period 'to' if database is non empty" in {
    Given("database is not empty")
      val now = LocalDateTime.now()
      val todayMidnight = now.toLocalDate().atStartOfDay()
      val todayMidday = now.toLocalDate().atTime(12, 0)

      insertIntoDb(
        List(
          ("past", 0, 0, now.minusMonths(1)),
          ("yesterday", 0, 0, now.minusDays(1)),
          ("today01", 0, 0, todayMidday.minusHours(3)),
          ("today02", 0, 0, todayMidday.plusHours(5)),
          ("todayMidnight", 0, 0, todayMidnight),
          ("tomorrowMidnight", 0, 0, todayMidnight.plusDays(1)),
          ("tomorrow", 0, 0, now.plusDays(1)),
          ("future", 0, 0, now.plusMonths(1))
        )
      )

    And("argument is period with 'to'")
      val argumentPeriod = model.Period(None, Some(todayMidday))

    When("the method is called with such argument")
      val result = dbStorage.locationStats(argumentPeriod).compile.toList.unsafeRunSync()

    Then("database entries older or equal to period 'to' (edge date included) are counted in statistics")
      val expectedStatistics = List(
        model.Location.Stats(now.toLocalDate().minusMonths(1).atStartOfDay(), 1),
        model.Location.Stats(now.toLocalDate().minusDays(1).atStartOfDay(), 1),
        model.Location.Stats(todayMidnight, 3)
      )

      result shouldBe expectedStatistics
  }

  it should "count all entries between period 'from' and 'to' (inclusively) if database is non empty" in {
    Given("database is not empty")
      val now = LocalDateTime.now()
      val todayMidnight = now.toLocalDate().atStartOfDay()
      val todayMidday = now.toLocalDate().atTime(12, 0)

      insertIntoDb(
        List(
          ("past", 0, 0, now.minusMonths(1)),
          ("yesterday", 0, 0, now.minusDays(1)),
          ("today01", 0, 0, todayMidday.minusHours(3)),
          ("today02", 0, 0, todayMidday.plusHours(5)),
          ("todayMidnight", 0, 0, todayMidnight),
          ("tomorrowMidnight", 0, 0, todayMidnight.plusDays(1)),
          ("tomorrow", 0, 0, now.plusDays(1)),
          ("future", 0, 0, now.plusMonths(1))
        )
      )

    And("argument is period with 'from' and 'to'")
      val argumentPeriod = model.Period(Some(todayMidday), Some(todayMidday.plusDays(1)))

    When("the method is called with such argument")
      val result = dbStorage.locationStats(argumentPeriod).compile.toList.unsafeRunSync()

    Then("database entries newer or equal to period 'from' and older or equal to period 'to' (edge dates included) are counted in statistics")
      val expectedStatistics = List(
        model.Location.Stats(todayMidnight, 3),
        model.Location.Stats(now.toLocalDate().plusDays(1).atStartOfDay(), 2)
      )

      result shouldBe expectedStatistics
  }


  extension[T <: model.Location.Base] (locations: List[T])
    def toSortedTuple4: List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]] = 
      locations.sortBy(_.id).map {
        case l: model.Location.WithCreatedField => (l.id, l.longitude, l.latitude, l.created)
        case l: model.Location.WithOptionalCreatedField => (l.id, l.longitude, l.latitude, l.created.get)
      }

    def toSortedTuple3: List[Tuple3[String, BigDecimal, BigDecimal]] = 
      locations.sortBy(_.id).map {l => (l.id, l.longitude, l.latitude) }
    
  private def sortedEntriesFromDb: List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]] =
    sql"SELECT location_id, location_longitude, location_latitude, location_created FROM locations"
      .query[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]]
      .to[List]
      .transact(transactor)
      .unsafeRunSync()
      .sortBy(_._1)

  private def insertIntoDb(entries: List[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]]): Unit =
    Update[Tuple4[String, BigDecimal, BigDecimal, LocalDateTime]] (
      "INSERT INTO locations (location_id, location_longitude, location_latitude, location_created) values (?, ?, ?, ?)"
    )
      .updateMany(entries)
      .transact(transactor)
      .unsafeRunSync()

  private def cleanDb(): Unit =
    sql"DELETE FROM locations"
      .update
      .run
      .transact(transactor)
      .unsafeRunSync()

  private def transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO](db.config.driver, db.config.userUrl, db.config.user.login, db.config.user.password)

  private def dbStorage = new DbStorage[IO](db.config)

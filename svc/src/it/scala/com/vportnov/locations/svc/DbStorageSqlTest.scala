package com.vportnov.locations.svc

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.should.Matchers._

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor

import java.time.LocalDateTime

import com.vportnov.locations.model


@DoNotDiscover
class DbStorageSqlTest extends AnyDbSpec with IOChecker:
  info("As a developer I need to verify that sql utils used in DbStorage")

  "DbStorage.sql.select.locations" should "not return a query if request contains both period and location ids" in {
    Given("period and ids arguments are not empty")
      val period = model.Period(Some(LocalDateTime.now()), None)
      period should not be empty

      val ids: model.Location.Ids = List("location123")
      ids should not be empty

    When("select.locations is called with such arguments")
      val result = DbStorage.sql.select.locations(period, ids)

    Then("None is returned")
      result shouldBe None
  }

  it should "create a valid query if period and ids are empty" in {
    Given("period and ids arguments are empty")
      val period = model.Period(None, None)
      period shouldBe empty

      val ids: model.Location.Ids = List.empty[model.Location.Id]
      ids shouldBe empty

    When("select.locations is called with such arguments")
      val result = DbStorage.sql.select.locations(period, ids)

    Then("some query is returned")
      result should not be empty

    And("the query is valid for database structure")
      check(result.get)
  }

  it should "create a valid query if period has 'from' date" in {
    Given("period has some 'from' date")
      val period = model.Period(Some(LocalDateTime.now()), None)
      period should not be empty

    And("ids is empty")
      val ids: model.Location.Ids = List.empty[model.Location.Id]
      ids shouldBe empty

    When("select.locations is called with such arguments")
      val result = DbStorage.sql.select.locations(period, ids)

    Then("some query is returned")
      result should not be empty

    And("the query is valid for database structure")
      check(result.get)
  }

  it should "create a valid query if period has 'to' date" in {
    Given("period has some 'to' date")
      val period = model.Period(None, Some(LocalDateTime.now()))
      period should not be empty

    And("ids is empty")
      val ids: model.Location.Ids = List.empty[model.Location.Id]
      ids shouldBe empty

    When("select.locations is called with such arguments")
      val result = DbStorage.sql.select.locations(period, ids)

    Then("some query is returned")
      result should not be empty

    And("the query is valid for database structure")
      check(result.get)
  }

  it should "create a valid query if period has both 'from' and 'to' dates" in {
    Given("period has both 'from' and 'to' dates")
      val period = model.Period(Some(LocalDateTime.now()), Some(LocalDateTime.now()))
      period should not be empty

    And("ids is empty")
      val ids: model.Location.Ids = List.empty[model.Location.Id]
      ids shouldBe empty

    When("select.locations is called with such arguments")
      val result = DbStorage.sql.select.locations(period, ids)

    Then("some query is returned")
      result should not be empty

    And("the query is valid for database structure")
      check(result.get)
  }

  it should "create a valid query if id list contains a single location id" in {
    Given("ids contain a single location id")
      val ids: model.Location.Ids = List("loction123")
      ids should not be empty

    And("period is empty")
      val period = model.Period(None, None)
      period shouldBe empty

    When("select.locations is called with such arguments")
      val result = DbStorage.sql.select.locations(period, ids)

    Then("some query is returned")
      result should not be empty

    And("the query is valid for database structure")
      check(result.get)
  }

  it should "create a valid query if id list contains several location id" in {
    Given("ids contain a single location id")
      val ids: model.Location.Ids = List("loction123", "location456", "oneMoreLocation")
      ids should not be empty

    And("period is empty")
      val period = model.Period(None, None)
      period shouldBe empty

    When("select.locations is called with such arguments")
      val result = DbStorage.sql.select.locations(period, ids)

    Then("some query is returned")
      result should not be empty

    And("the query is valid for database structure")
      check(result.get)
  }

  "DbStorage.sql.select.stats" should "create a valid query if period is empty" in {
    Given("period is empty")
      val period = model.Period(None, None)
      period shouldBe empty

    When("select.stats is called with such arguments")
      val result = DbStorage.sql.select.stats(period)

    Then("the query is valid for database structure")
      check(result)
  }

  it should "create a valid query if period has 'from' date" in {
    Given("period has some 'from' date")
      val period = model.Period(Some(LocalDateTime.now()), None)
      period should not be empty

    When("select.stats is called with such arguments")
      val result = DbStorage.sql.select.stats(period)

    Then("the query is valid for database structure")
      check(result)
  }

  it should "create a valid query if period has 'to' date" in {
    Given("period has some 'to' date")
      val period = model.Period(None, Some(LocalDateTime.now()))
      period should not be empty

    When("select.stats is called with such arguments")
      val result = DbStorage.sql.select.stats(period)

    Then("the query is valid for database structure")
      check(result)
  }

  it should "create a valid query if period has both 'from' and 'to' dates" in {
    Given("period has both 'from' and 'to' dates")
      val period = model.Period(Some(LocalDateTime.now()), Some(LocalDateTime.now()))
      period should not be empty

    When("select.stats is called with such arguments")
      val result = DbStorage.sql.select.stats(period)

    Then("the query is valid for database structure")
      check(result)
  }

  override def transactor: Transactor[IO] =
  Transactor.fromDriverManager[IO](db.config.driver, db.config.userUrl, db.config.user.login, db.config.user.password)

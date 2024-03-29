package com.vportnov.locations.svc

import org.scalatest.DoNotDiscover
import org.scalatest.EitherValues._
import org.scalatest.matchers.should.Matchers._

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor

import java.time.LocalDateTime

import com.vportnov.locations.model
import com.vportnov.locations.utils.ServerError


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

    Then("illegal argument error is returned")
      result.left.value shouldBe a [ServerError]
      result.left.value.asInstanceOf[ServerError].kind shouldBe ServerError.Kind.IllegalArgument
  }

  it should "create a valid query if period and ids are empty" in {
    Given("period and ids arguments are empty")
      val period = model.Period(None, None)
      period shouldBe empty

      val ids: model.Location.Ids = List.empty[model.Location.Id]
      ids shouldBe empty

    When("select.locations is called with such arguments")
      val result = DbStorage.sql.select.locations(period, ids)

    Then("the query is returned which is valid for database structure")
      check(result.value)
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

    Then("the query is returned which is valid for database structure")
      check(result.value)
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

    Then("the query is returned which is valid for database structure")
      check(result.value)
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

    Then("the query is returned which is valid for database structure")
      check(result.value)
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

    Then("the query is returned which is valid for database structure")
      check(result.value)
  }

  it should "create a valid query if id list contains several location ids" in {
    Given("ids contain a single location id")
      val ids: model.Location.Ids = List("loction123", "location456", "oneMoreLocation")
      ids should not be empty

    And("period is empty")
      val period = model.Period(None, None)
      period shouldBe empty

    When("select.locations is called with such arguments")
      val result = DbStorage.sql.select.locations(period, ids)

    Then("the query is returned which is valid for database structure")
      check(result.value)
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

  "DbStorage.sql.insert.locations" should "create a valid query if location list contains a single value without a created date" in {
    Given("location list contains a single value without a created date")
      val locations = List(model.Location.WithOptionalCreatedField("location123", 0, 0, None))
      locations should have length 1
      locations.head.created shouldBe empty

    When("insert.locations is called with such list")
      val result = DbStorage.sql.insert.locations(locations)
    
    Then("the query is returned which is valid for database structure")
      check(result.value)
  }

  it should "create a valid query if location list contains a single value with a created date" in {
    Given("location list contains a single value with a created date")
      val locations = List(model.Location.WithOptionalCreatedField("location123", 0, 0, Some(LocalDateTime.now())))
      locations should have length 1
      locations.head.created should not be empty

    When("insert.locations is called with such list")
      val result = DbStorage.sql.insert.locations(locations)
    
    Then("the query is returned which is valid for database structure")
      check(result.value)
  }

  it should "create a valid query if location list contains several values with mixed state of created date" in {
    Given("location list contains several values")
      val locations =
        List(
          model.Location.WithOptionalCreatedField("location123", 0, 0, Some(LocalDateTime.now())),
          model.Location.WithOptionalCreatedField("location456", -3, 5, None),
          model.Location.WithOptionalCreatedField("location789", 180, 90, None)
        )

      locations should have length 3

    And("some of them have a create date")
      locations.head.created should not be empty

    And("some of them don't have a create date")
      locations.tail.head.created shouldBe empty
      locations.tail.tail.head.created shouldBe empty

    When("insert.locations is called with such list")
      val result = DbStorage.sql.insert.locations(locations)
    
    Then("the query is returned which is valid for database structure")
      check(result.value)
  }

  it should "not create a query if location list is empty" in {
    Given("location list is empty")
      val locations = List()
      locations shouldBe empty

    When("insert.locations is called with such list")
      val result = DbStorage.sql.insert.locations(locations)
    
    Then("illegal argument error is returned")
      result.left.value shouldBe a [ServerError]
      result.left.value.asInstanceOf[ServerError].kind shouldBe ServerError.Kind.IllegalArgument
  }

  "DbStorage.sql.update.locations" should "create a valid query if location list contains a single value" in {
    Given("location list contains a single value")
      val locations = List(model.Location.WithoutCreatedField("location123", 0, 0))
      locations should have length 1

    When("update.locations is called with such list")
      val result = DbStorage.sql.update.locations(locations)
    
    Then("the query is returned which is valid for database structure")
      check(result.value)
  }

  it should "create a valid query if location list contains several values" in {
    Given("location list contains several values")
      val locations =
        List(
          model.Location.WithoutCreatedField("location123", 0, 0),
          model.Location.WithoutCreatedField("location456", -3, 5),
          model.Location.WithoutCreatedField("location789", 180, 90)
        )

      locations should have length 3

    When("update.locations is called with such list")
      val result = DbStorage.sql.update.locations(locations)
    
    Then("the query is returned which is valid for database structure")
      check(result.value)
  }

  it should "not create a query if location list is empty" in {
    Given("location list is empty")
      val locations = List()
      locations shouldBe empty

    When("update.locations is called with such list")
      val result = DbStorage.sql.update.locations(locations)
    
    Then("illegal argument error is returned")
      result.left.value shouldBe a [ServerError]
      result.left.value.asInstanceOf[ServerError].kind shouldBe ServerError.Kind.IllegalArgument
  }

  "DbStorage.sql.delete.locations" should "create a valid query if location id list contains a single value" in {
    Given("location id list contains a single value")
      val ids = List("location123")
      ids should have length 1

    When("delete.locations is called with such list")
      val result = DbStorage.sql.delete.locations(ids)
    
    Then("the query is returned which is valid for database structure")
      check(result.value)
  }

  it should "create a valid query if location id list contains several values" in {
    Given("location id list contains several values")
      val ids = List("location123", "location456", "location789")
      ids should have length 3

    When("delete.locations is called with such list")
      val result = DbStorage.sql.delete.locations(ids)
    
    Then("the query is returned which is valid for database structure")
      check(result.value)
  }

  it should "create a valid query if location id list is empty" in {
    Given("location id list contains several values")
      val ids = List.empty[model.Location.Id]
      ids shouldBe empty

    When("delete.locations is called with such list")
      val result = DbStorage.sql.delete.locations(ids)
    
    Then("illegal argument error is returned")
      result.left.value shouldBe a [ServerError]
      result.left.value.asInstanceOf[ServerError].kind shouldBe ServerError.Kind.IllegalArgument
  }

  override def transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO](db.config.driver, db.config.userUrl, db.config.user.login, db.config.user.password)

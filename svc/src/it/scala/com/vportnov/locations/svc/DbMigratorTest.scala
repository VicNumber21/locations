package com.vportnov.locations.svc

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.should.Matchers._

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import org.flywaydb.core.api.FlywayException


@DoNotDiscover
class DbMigratorTest extends AnyDbSpec:
  "DbMigrator" should "apply all sql scripts on the first run" in {
    Given("DbMigrator is running on clean database")

    When("migration is done")
      val scriptCount = DbMigrator.migrate[IO](db.config).unsafeRunSync()

    Then("all sql scripts should be applied")
      scriptCount shouldBe 3
  }

  it should "do nothing on the next run" in {
    Given("DbMigrator is running on existing database")

    When("migration is done")
      val scriptCount = DbMigrator.migrate[IO](db.config).unsafeRunSync()

    Then("no sql script should be applied")
      scriptCount shouldBe 0
  }

  it should "throw in case of wrong database config" in {
    Given("database config is broken ")
      val brokenConfig = db.config.copy(url = "Bad driver")

    When("migration attempts to run")

    Then("exception is thron")
      an[FlywayException] should be thrownBy DbMigrator.migrate[IO](brokenConfig).unsafeRunSync()
  }

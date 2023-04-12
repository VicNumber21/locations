package com.vportnov.locations.svc

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.should.Matchers._

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import org.slf4j.LoggerFactory

import org.flywaydb.core.api.FlywayException


@DoNotDiscover
class DbMigratorTest extends AnyDbSpec:
  info("As a developer I need to verify that initial and subsequent migrations pass sucessfully")

  "DbMigrator" should "apply all sql scripts on the first run" in {
    logger.info("\n\nbegin first migration")

    Given("DbMigrator is running on clean database")

    When("migration is done")
      val scriptCount = DbMigrator.migrate[IO](db.config).unsafeRunSync()

    Then("all sql scripts should be applied")
      scriptCount shouldBe 3

    logger.info("\nend first migration\n")
  }

  it should "do nothing on the next run" in {
    logger.info("\n\nbegin next migration")

    Given("DbMigrator is running on existing database")

    When("migration is done")
      val scriptCount = DbMigrator.migrate[IO](db.config).unsafeRunSync()

    Then("no sql script should be applied")
      scriptCount shouldBe 0

    logger.info("\nend next migration\n")
  }

  it should "throw in case of wrong database config" in {
    Given("database config is broken ")
      val brokenConfig = db.config.copy(url = "Bad driver")

    When("migration attempts to run")

    Then("exception is thron")
      an[FlywayException] should be thrownBy DbMigrator.migrate[IO](brokenConfig).unsafeRunSync()
  }

  private val logger = LoggerFactory.getLogger(classOf[DbMigratorTest])

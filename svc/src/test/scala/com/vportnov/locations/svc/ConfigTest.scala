package com.vportnov.locations.svc

import org.scalatest.matchers.should.Matchers._
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec

import cats.effect.IO
import cats.effect.unsafe.implicits.global

import scala.jdk.CollectionConverters.MapHasAsJava
import com.comcast.ip4s._


def setEnv(newEnv: Map[String, String]) =
  val javaNewEnv = newEnv.asJava
  val env = System.getenv().asInstanceOf[java.util.Map[String, String]];
  val cl = env.getClass();
  val field = cl.getDeclaredField("m");
  field.setAccessible(true);
  val writableEnv = field.get(env).asInstanceOf[java.util.Map[String, String]];
  writableEnv.putAll(javaNewEnv)

class ConfigDefaultTestInFork extends AnyFlatSpec with GivenWhenThen:
  info("As a developer I need Config class to load configuration of svc application")

  "Config" should "be able to load defaul configuration" in {
    Given("default config exists")

    When("default config is loaded")
      val settings = Config.loadSettings[IO].unsafeRunSync()
    
    Then("loadded settings are equal to ones in conf file")
      settings.grpc.port shouldBe Port.fromInt(9090).get
      settings.db.driver shouldBe "org.postgresql.Driver"
      settings.db.url shouldBe "jdbc:postgresql://db:5432"
      settings.db.name shouldBe "locations"
      settings.db.user.login shouldBe "locator"
      settings.db.user.password shouldBe "locator"
      settings.db.admin.login shouldBe "user"
      settings.db.admin.password shouldBe "pswd"
  }

class ConfigOverrideAllTestInFork extends AnyFlatSpec with GivenWhenThen:
  "Config" should "be allow to overrid all default settings by env variables" in {
    Given("env variables override default settings")
      val envVars = Map(
        "GRPC_PORT" -> "9999",
        "DB_DRIVER" -> "some.driver",
        "DB_URL" -> "jdbc.some.url://host:8888",
        "DB_NAME" -> "glonass",
        "DB_USER" -> "kid",
        "DB_USER_PASSWORD" -> "weak",
        "DB_ADMIN" -> "god",
        "DB_ADMIN_PASSWORD" -> "super strong"
      )
      setEnv(envVars)

    When("default config is loaded")
      val settings = Config.loadSettings[IO].unsafeRunSync()
    
    Then("loadded settings are equal to ones in conf file")
      settings.grpc.port shouldBe Port.fromString(envVars("GRPC_PORT")).get
      settings.db.driver shouldBe envVars("DB_DRIVER")
      settings.db.url shouldBe envVars("DB_URL")
      settings.db.name shouldBe envVars("DB_NAME")
      settings.db.user.login shouldBe envVars("DB_USER")
      settings.db.user.password shouldBe envVars("DB_USER_PASSWORD")
      settings.db.admin.login shouldBe envVars("DB_ADMIN")
      settings.db.admin.password shouldBe envVars("DB_ADMIN_PASSWORD")
  }

class ConfigOverrideSomeTestInFork extends AnyFlatSpec with GivenWhenThen:
  "Config" should "be allow to overrid some default settings by env variables" in {
    Given("env variables override default settings")
      val envVars = Map(
        "GRPC_PORT" -> "9999",
        "DB_DRIVER" -> "some.driver",
        "DB_URL" -> "jdbc.some.url://host:8888",
        "DB_NAME" -> "glonass"
      )
      setEnv(envVars)

    When("default config is loaded")
      val settings = Config.loadSettings[IO].unsafeRunSync()
    
    Then("loadded settings are equal to ones in conf file")
      settings.grpc.port shouldBe Port.fromString(envVars("GRPC_PORT")).get
      settings.db.driver shouldBe envVars("DB_DRIVER")
      settings.db.url shouldBe envVars("DB_URL")
      settings.db.name shouldBe envVars("DB_NAME")
      settings.db.user.login shouldBe "locator"
      settings.db.user.password shouldBe "locator"
      settings.db.admin.login shouldBe "user"
      settings.db.admin.password shouldBe "pswd"
  }

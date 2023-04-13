package com.vportnov.locations.api

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
  info("As a developer I need Config class to load configuration of api application")

  "Config" should "be able to load defaul configuration" in {
    Given("default config exists")

    When("default config is loaded")
      val settings = Config.loadSettings[IO].unsafeRunSync()
    
    Then("loadded settings are equal to ones in conf file")
      settings.http.host shouldBe Host.fromString("0.0.0.0").get
      settings.http.port shouldBe Port.fromInt(8080).get
      settings.grpc.host shouldBe Host.fromString("svc").get
      settings.grpc.port shouldBe Port.fromInt(9090).get
  }

class ConfigOverrideAllTestInFork extends AnyFlatSpec with GivenWhenThen:
  "Config" should "be allow to overrid all default settings by env variables" in {
    Given("env variables override default settings")
      val envVars = Map(
        "HTTP_HOST" -> "http.host",
        "HTTP_PORT" -> "8888",
        "GRPC_HOST" -> "grpc.host",
        "GRPC_PORT" -> "9999"
      )
      setEnv(envVars)

    When("default config is loaded")
      val settings = Config.loadSettings[IO].unsafeRunSync()
    
    Then("loadded settings are equal to ones in conf file")
      settings.http.host shouldBe Host.fromString(envVars("HTTP_HOST")).get
      settings.http.port shouldBe Port.fromString(envVars("HTTP_PORT")).get
      settings.grpc.host shouldBe Host.fromString(envVars("GRPC_HOST")).get
      settings.grpc.port shouldBe Port.fromString(envVars("GRPC_PORT")).get
  }

class ConfigOverrideSomeTestInFork extends AnyFlatSpec with GivenWhenThen:
  "Config" should "be allow to overrid some default settings by env variables" in {
    Given("env variables override default settings")
      val envVars = Map(
        "HTTP_HOST" -> "http.host",
        "GRPC_PORT" -> "9999"
      )
      setEnv(envVars)

    When("default config is loaded")
      val settings = Config.loadSettings[IO].unsafeRunSync()
    
    Then("loadded settings are equal to ones in conf file")
      settings.http.host shouldBe Host.fromString(envVars("HTTP_HOST")).get
      settings.http.port shouldBe Port.fromInt(8080).get
      settings.grpc.host shouldBe Host.fromString("svc").get
      settings.grpc.port shouldBe Port.fromString(envVars("GRPC_PORT")).get
  }

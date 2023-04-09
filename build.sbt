import sys.process._
import sbt.Keys._

import sbtprotoc.ProtocPlugin.ProtobufConfig


lazy val utils =
  (project in file("./utils"))
    .settings(settings.common)
    .settings(
      libraryDependencies ++= Seq(
        libs.catsEffect,
        libs.fs2Core,
        libs.log4catsSLF4J
      )
    )

lazy val model =
  (project in file("./model"))
    .settings(settings.common)
    .settings(
      libraryDependencies ++= Seq(
        libs.catsEffect,
        libs.fs2Core
      )
    )
    .settings(
      libraryDependencies += libs.scalatest % Test
    )
    .dependsOn(utils)

lazy val grpc =
  (project in file("./grpc"))
    .settings(settings.common)
    .settings(
      scalapbCodeGeneratorOptions += CodeGeneratorOption.FlatPackage
    )
    .settings(
      libraryDependencies ++= Seq(
        libs.scalapbRuntime % ProtobufConfig
      )
    )
    .enablePlugins(Fs2Grpc)
    .dependsOn(model)
    .dependsOn(utils)

lazy val api =
  (project in file("./api"))
    .settings(settings.common)
    .settings(
      libraryDependencies ++=
        Seq(
          libs.grpcNetty,

          libs.http4sDsl,
          libs.http4sEmberServer,

          libs.pureConfig,

          libs.tapitCore,
          libs.tapirJsonCirce,
          libs.tapirHttp4sServer,
          libs.tapirSwaggerUiBundle,

          libs.slf4jSimple
        )
    )
    .settings(
      packCopyDependenciesUseSymbolicLinks := false
    )
    .settings(
      docker / dockerfile := NativeDockerfile(file("./api") / "Dockerfile"),
      docker / imageNames := Seq(
        ImageName(
          namespace = Some(settings.docker.namespace),
          repository = settings.docker.name.api,
          tag = Some("latest")
        ),
        ImageName(
          namespace = Some(settings.docker.namespace),
          repository = settings.docker.name.api,
          tag = Some(version.value)
        )
      )
    )
    .settings(
      build := Def.sequential(
        Compile / compile,
        Compile / packCopyDependencies,
        docker
      ).value
    )
    .enablePlugins(DockerPlugin)
    .enablePlugins(PackPlugin)
    .dependsOn(utils)
    .dependsOn(model)
    .dependsOn(grpc)

lazy val svc =
  (project in file("./svc"))
    .settings(settings.common)
    .settings(
      libraryDependencies ++=
        Seq(
          libs.doobieCore,
          libs.doobiePostgres,
          libs.flywayCore,
          libs.grpcNetty,
          libs.pureConfig,
          libs.slf4jSimple
        )
    )
    .settings(
      packCopyDependenciesUseSymbolicLinks := false
    )
    .settings(
      docker / dockerfile := NativeDockerfile(file("./svc") / "Dockerfile"),
      docker / imageNames := Seq(
        ImageName(
          namespace = Some(settings.docker.namespace),
          repository = settings.docker.name.svc,
          tag = Some("latest")
        ),
        ImageName(
          namespace = Some(settings.docker.namespace),
          repository = settings.docker.name.svc,
          tag = Some(version.value)
        )
      )
    )
    .settings(
      build := Def.sequential(
        Compile / compile,
        Compile / packCopyDependencies,
        docker
      ).value
    )
    .configs(IntegrationTest)
    .settings(
      Defaults.itSettings,
      IntegrationTest / parallelExecution := false,
      libraryDependencies ++=
        Seq(
          libs.testcontainersScalatest % IntegrationTest,
          // TODO check that all deps needed
          libs.scalatest % IntegrationTest,
          libs.scalacheck % IntegrationTest,
          libs.scalastick % IntegrationTest,
          libs.doobieScalatest % IntegrationTest
        )
    )
    .enablePlugins(DockerPlugin)
    .enablePlugins(PackPlugin)
    .dependsOn(model)
    .dependsOn(grpc)
    .dependsOn(utils)

lazy val locations =
  Project("locations", file("."))
    .settings(settings.common)
    .aggregate(grpc, api, svc, model, utils)


lazy val build = taskKey[Unit]("production build sequence")

lazy val settings =
  new {
    val common =
      Seq(
        organization := "com.vportnov",
        version := "1.0.0",
        scalaVersion := "3.2.2",
        scalacOptions :=
          Seq(
            "-encoding", "utf8",
            "-feature",
            "-deprecation",
            "-unchecked",
            "-Werror"
          )
      )

    object docker {
      val namespace = "vportnov"

      object name {
        val api = "locations-api"
        val svc = "locations-svc"
      }
    }
  }

lazy val libs =
  new {
    object version {
      val cats = "3.4.8"
      val doobie = "1.0.0-RC2"
      val flyway = "9.16.1"
      val fs2 = "3.6.1"
      val grpcNetty = "1.53.0"
      val http4s = "0.23.18"
      val log4cats = "2.5.0"
      val pureConfig = "0.17.2"
      val scalacheck = "3.2.15.0"
      val scalatest = "3.2.15"
      val scalapbRuntime = "0.11.13"
      val slf4j = "2.0.6"
      val tapir = "1.2.8"
      val testcontainers = "0.40.12"
    }

    val catsEffect = "org.typelevel" %% "cats-effect" % version.cats

    val doobieCore = "org.tpolecat" %% "doobie-core" % version.doobie
    val doobiePostgres = "org.tpolecat" %% "doobie-postgres"  % version.doobie
    val doobieScalatest = "org.tpolecat" %% "doobie-scalatest" % version.doobie

    val flywayCore = "org.flywaydb" %  "flyway-core" % version.flyway
    val fs2Core = "co.fs2" %% "fs2-core" % version.fs2

    val grpcNetty = "io.grpc" % "grpc-netty" % version.grpcNetty

    val http4sDsl = "org.http4s" %% "http4s-dsl" % version.http4s
    val http4sCirce = "org.http4s" %% "http4s-circe" % version.http4s
    val http4sEmberServer = "org.http4s" %% "http4s-ember-server" % version.http4s
    
    val log4catsSLF4J = "org.typelevel" %% "log4cats-slf4j" % version.log4cats

    val pureConfig = "com.github.pureconfig" %% "pureconfig-core" % version.pureConfig

    val scalacheck = "org.scalatestplus" %% "scalacheck-1-17" % version.scalacheck
    val scalastick = "org.scalactic" %% "scalactic" % version.scalatest
    val scalatest = "org.scalatest" %% "scalatest" % version.scalatest

    val scalapbRuntime = "com.thesamet.scalapb" %% "scalapb-runtime" % version.scalapbRuntime
    val slf4jSimple = "org.slf4j" % "slf4j-simple" % version.slf4j

    val tapitCore = "com.softwaremill.sttp.tapir" %% "tapir-core" % version.tapir
    val tapirJsonCirce = "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % version.tapir
    val tapirHttp4sServer = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % version.tapir
    val tapirSwaggerUiBundle = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % version.tapir

    val testcontainersScalatest = "com.dimafeng" %% "testcontainers-scala-scalatest" % version.testcontainers
  }

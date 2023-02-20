import sys.process._
import sbt.Keys._

lazy val commonSetting = Seq(
  organization := "com.vportnov",
  version := "1.0.0",
  scalaVersion := "3.2.1",
  scalacOptions := Seq(
    "-encoding", "utf8",
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Werror"
  )
)

lazy val dockerNamespace = "vportnov"
lazy val dockerApiName = "locations-api"
lazy val dockerDbName = "locations-db"


lazy val api = (project in file("./api"))
  .settings(commonSetting)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % "0.23.18",
      "org.http4s" %% "http4s-ember-server" % "0.23.18",

      "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.2.8",
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.2.8",
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "1.2.8",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % "1.2.8"
    )
  )
  .settings(
    packCopyDependenciesUseSymbolicLinks := false
  )
  .settings(
    docker / dockerfile := NativeDockerfile(file("./api") / "Dockerfile"),
    docker / imageNames := Seq(
      ImageName(
        namespace = Some(dockerNamespace),
        repository = dockerApiName,
        tag = Some("latest")
      ),
      ImageName(
        namespace = Some(dockerNamespace),
        repository = dockerApiName,
        tag = Some(version.value)
      )
    )
  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(PackPlugin)

lazy val svc = (project in file("./svc"))
  .settings(commonSetting)

lazy val db = (project in file("./db"))
  .settings(commonSetting)
  .settings(
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.15" % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.15" % Test,
    libraryDependencies += "org.scalatestplus" %% "scalacheck-1-17" % "3.2.15.0" % Test,

    libraryDependencies ++= Seq(
      "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.40.12" % Test,
      "org.slf4j" % "slf4j-api" % "2.0.6" % Test,
      "org.slf4j" % "slf4j-simple" % "2.0.6" % Test
    ),

    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core"      % "1.0.0-RC2" % Test,
      "org.tpolecat" %% "doobie-postgres"  % "1.0.0-RC2" % Test,
      "org.tpolecat" %% "doobie-scalatest" % "1.0.0-RC2" % Test
    )
  )
  .settings(
    docker / dockerfile := NativeDockerfile(file("./db") / "Dockerfile"),
    docker / imageNames := Seq(
      ImageName(
        namespace = Some(dockerNamespace),
        repository = dockerDbName,
        tag = Some("latest")
      ),
      ImageName(
        namespace = Some(dockerNamespace),
        repository = dockerDbName,
        tag = Some(version.value)
      )
    )
  )
  .enablePlugins(DockerPlugin)

lazy val locations = Project("locations", file("."))
  .aggregate(api, svc, db)
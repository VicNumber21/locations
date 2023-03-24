package com.vportnov.locations.svc

import cats.effect.{ IO, IOApp, ExitCode }
import org.slf4j.LoggerFactory

import com.vportnov.locations.svc.GrpcServer
import com.vportnov.locations.svc.DbMigrator
import com.vportnov.locations.svc.Config


object App extends IOApp:
  val logger = LoggerFactory.getLogger(classOf[App])

  def run(args: List[String]): IO[ExitCode] =
    for 
      settings <- Config.loadSettings[IO]
      migrationsCount <- DbMigrator.migrate[IO](settings.db)
      _ <- IO.delay { logger.info(s"Applied migrations count: ${migrationsCount}") }
      server = new GrpcServer[IO](settings)
      exitCode <- server.run
    yield exitCode
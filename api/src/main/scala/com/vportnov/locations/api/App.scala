package com.vportnov.locations.api

import cats.effect._
import com.comcast.ip4s._
import org.http4s.ember.server._

import com.vportnov.locations.api.ApiConfig
import com.vportnov.locations.api.Service


object App extends IOApp:
  def run(args: List[String]): IO[ExitCode] =
    for 
      apiConfig <- ApiConfig.load[IO]
      exitCode <- server(apiConfig).use(_ => IO.never.as(ExitCode.Success))
    yield exitCode

  private def server(apiConfig: ApiConfig) =
    EmberServerBuilder
    .default[IO]
    .withHost(apiConfig.host)
    .withPort(apiConfig.port)
    .withHttpApp(Service.app)
    .build
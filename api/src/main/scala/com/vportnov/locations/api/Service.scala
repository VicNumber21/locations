package com.vportnov.locations.api

import cats.effect._
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import org.http4s.server.Router

import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import com.vportnov.locations.api.LocationsRoutes
import com.vportnov.locations.api.StorageGrpc


object Service:
  def app: HttpApp[IO] = Router("/" -> (routes)).orNotFound

  private val storage = new StorageGrpc[IO]
  private val locationsRoutes = new LocationsRoutes(storage)
  private val swaggerRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(SwaggerInterpreter().fromEndpoints[IO](LocationsRoutes.endpoints, "Locations Service", "1.0.0"))

  private val routes: HttpRoutes[IO] = 
    locationsRoutes.routes <+>
    swaggerRoutes
package com.vportnov.locations.api

import cats.effect._
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import org.http4s.server.Router

import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import com.vportnov.locations.api.LocationsRoutes
import com.vportnov.locations.model.StorageExt


final class HttpService[F[_]: Async](storage: StorageExt[F]):
  def app: HttpApp[F] = Router("/" -> (routes)).orNotFound

  private val locationsRoutes = new LocationsRoutes(storage)
  private val swaggerRoutes: HttpRoutes[F] =
    Http4sServerInterpreter[F](locationsRoutes.options).toRoutes(SwaggerInterpreter().fromEndpoints[F](LocationsRoutes.endpoints, "Locations Service", "1.0.0"))

  private val routes: HttpRoutes[F] = 
    locationsRoutes.routes <+>
    swaggerRoutes
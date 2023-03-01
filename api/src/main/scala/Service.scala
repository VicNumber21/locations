package com.vportnov.locations.api

import cats.effect._
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import org.http4s.server.Router

import java.time.LocalDateTime

import com.vportnov.locations.api.types.api._
import com.vportnov.locations.api.types.structures._
import com.vportnov.locations.api.Routes


object Service:
  def app: HttpApp[IO] = Router("/" -> (routes)).orNotFound

  private val readLogic: (PeriodQuery) => IO[read.Response] =
    case PeriodQuery(None, None) => IO(List(LocationResponse("NoDates", 0, 0, LocalDateTime.now())))
    case PeriodQuery(Some(date), None) => IO(List(LocationResponse("OnlyFrom", 0, 0, date)))
    case PeriodQuery(None, Some(date)) => IO(List(LocationResponse("OnlyTo", 0, 0, date)))
    case PeriodQuery(Some(_), Some(_)) => IO(List(LocationResponse("BothFromAndTo", 0, 0, LocalDateTime.now())))
    case _ => IO(List.empty[LocationResponse])

  private val createLogic: (create.Request) => IO[create.Response] = requests => IO(requests.map {
    case LocationRequest(id, longitude, latitude, None) => LocationResponse(id, longitude, latitude, LocalDateTime.now())
    case LocationRequest(id, longitude, latitude, Some(date)) => LocationResponse(id, longitude, latitude, date)
  })

  private val routes: HttpRoutes[IO] = 
    Routes.getCreateRoute(createLogic) <+>
    Routes.getReadRoute(readLogic) <+>
    Routes.swaggerUIRoutes
package com.vportnov.locations.api

import cats.effect._
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.HttpApp
import org.http4s.server.Router

import java.time.LocalDateTime

import fs2.Stream
import io.circe.syntax._
import io.circe.generic.auto._

import com.vportnov.locations.api.types.api._
import com.vportnov.locations.api.types.structures._
import com.vportnov.locations.api.Routes


object Service:
  def app: HttpApp[IO] = Router("/" -> (routes)).orNotFound

  private val readLogic: (read.Request) => IO[read.Response] = (periodQuery, ids) =>
    val location @ LocationResponse(baseId, _, _, created) = periodQuery match
      case PeriodQuery(None, None) => LocationResponse("NoDates", 0, 0, LocalDateTime.now())
      case PeriodQuery(Some(date), None) => LocationResponse("OnlyFrom", 0, 0, date)
      case PeriodQuery(None, Some(date)) => LocationResponse("OnlyTo", 0, 0, date)
      case PeriodQuery(Some(_), Some(_)) => LocationResponse("BothFromAndTo", 0, 0, LocalDateTime.now())

    val response = if ids.length > 0
                      then ids.map(id => LocationResponse(s"$baseId-$id", 0, 0, created))
                      else List(location)
    IO(response)

  private val readStreamLogic: (read.Request) => IO[Stream[IO, Byte]] = request =>
    for {
      list <- readLogic(request)
      result = Stream.eval(list.pure[IO])
        .map(_.asJson.noSpaces)
      bytes = result.through(fs2.text.utf8.encode)
    } yield bytes
  
  private val readOneLogic: (readOne.Request) => IO[readOne.Response] = (id) =>
    for {
      list <- readLogic(PeriodQuery(None, None), List(id))
    } yield list.head

  private val createLogic: (create.Request) => IO[create.Response] = requests => IO(requests.map {
    case LocationCreateRequest(id, longitude, latitude, None) => LocationResponse(id, longitude, latitude, LocalDateTime.now())
    case LocationCreateRequest(id, longitude, latitude, Some(date)) => LocationResponse(id, longitude, latitude, date)
  })

  private val createOneLogic: (createOne.Request) => IO[createOne.Response] = (id, request) =>
    for {
      list <- createLogic(List(LocationCreateRequest(id, request.longitude, request.latitude, request.created)))
    } yield list.head

  private val routes: HttpRoutes[IO] = 
    Routes.getCreateRoute(createLogic) <+>
    Routes.getCreateOneRoute(createOneLogic) <+>
    Routes.getReadRoute(readLogic) <+>
    Routes.getReadStreamRoute(readStreamLogic) <+>
    Routes.getReadOneRoute(readOneLogic) <+>
    Routes.swaggerUIRoutes
package com.vportnov.locations.api

import cats.effect.IO
import org.http4s.HttpRoutes
import sttp.tapir._
import sttp.model.StatusCode
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._

import com.vportnov.locations.api.types.api._
import com.vportnov.locations.api.types.structures._


object Routes:
  def getCreateRoute(logic: (create.Request) => IO[create.Response]): HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(createEndpoint.serverLogicSuccess(logic))

  def getCreateOneRoute(logic: (createOne.Request) => IO[createOne.Response]): HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(createOneEndpoint.serverLogicSuccess(logic))

  def getReadRoute(logic: (read.Request) => IO[read.Response]): HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(readEndpoint.serverLogicSuccess(logic))

  def getReadOneRoute(logic: (readOne.Request) => IO[readOne.Response]): HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(readOneEndpoint.serverLogicSuccess(logic))

  def swaggerUIRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(SwaggerInterpreter().fromEndpoints[IO](allEnpoints, "Locations Service", "1.0.0"))

  private val periodQuery: EndpointInput[PeriodQuery] =
    query[OptionalDateTime]("from")
    .and(query[OptionalDateTime]("to"))
    .mapTo[PeriodQuery]

  val idsQuery: EndpointInput[IdsQuery] =
    query[IdsQuery]("ids")
      .validateIterable(meta.id.validator)

  private val baseEndpoint = endpoint
    .in("api" / "v1.0" / "locations")
    .errorOut(statusCode)

  private val createEndpoint: PublicEndpoint[create.Request, StatusCode, create.Response, Any] = baseEndpoint
    .post
    .in(jsonBody[create.Request])
    .out(jsonBody[create.Response])

  private val createOneEndpoint: PublicEndpoint[createOne.Request, StatusCode, createOne.Response, Any] = baseEndpoint
    .post
    .in(path[createOne.RequestPath]("id"))
    .in(jsonBody[createOne.RequestBody])
    .out(jsonBody[createOne.Response])

  private val readEndpoint: PublicEndpoint[read.Request, StatusCode, read.Response, Any] = baseEndpoint
    .get
    .in(periodQuery).in(idsQuery)
    .out(jsonBody[read.Response])

  private val readOneEndpoint: PublicEndpoint[readOne.Request, StatusCode, readOne.Response, Any] = baseEndpoint
    .get
    .in(path[readOne.Request]("id"))
    .out(jsonBody[readOne.Response])

  // TODO update types and add logic: should get list of locations wihtout time since time cannot be modified
  private val updateEndpoint: PublicEndpoint[update.Request, StatusCode, update.Response, Any] = baseEndpoint
    .put
    .in(jsonBody[create.Request])
    .out(jsonBody[create.Response])

  // TODO update types and add logic: should get list of ids and return 204
  private val deleteEndpoint: PublicEndpoint[create.Request, StatusCode, create.Response, Any] = baseEndpoint
    .delete
    .in(jsonBody[create.Request])
    .out(jsonBody[create.Response])

  
  private val allEnpoints =
    List(
      createEndpoint,
      createOneEndpoint,
      readEndpoint,
      readOneEndpoint,
      updateEndpoint,
      deleteEndpoint
    )
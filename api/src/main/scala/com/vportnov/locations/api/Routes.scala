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

  private val idsQuery: EndpointInput[IdsQuery] =
    query[IdsQuery]("id")
      .validateIterable(meta.id.validator)
  
  private val idPath: EndpointInput[Id] =
    path[Id]("id")
      .validate(meta.id.validator)

  private val baseEndpoint = endpoint
    .in("api" / "v1.0" / "locations")
    .errorOut(statusCode)

  private val createEndpoint: PublicEndpoint[create.Request, StatusCode, create.Response, Any] = baseEndpoint
    .post
    .description("Create locations in batch")
    .in(jsonBody[create.Request])
    .out(jsonBody[create.Response])
    .out(statusCode(StatusCode.Created))

  private val createOneEndpoint: PublicEndpoint[createOne.Request, StatusCode, createOne.Response, Any] = baseEndpoint
    .post
    .description("Create a single location")
    .in(idPath)
    .in(jsonBody[createOne.RequestBody])
    .out(jsonBody[createOne.Response])
    .out(statusCode(StatusCode.Created))

  private val readEndpoint: PublicEndpoint[read.Request, StatusCode, read.Response, Any] = baseEndpoint
    .get
    .description("Get list of locations: all, particular ids or before or after or between dates")
    .in(periodQuery).in(idsQuery)
    .out(jsonBody[read.Response])

  private val readOneEndpoint: PublicEndpoint[readOne.Request, StatusCode, readOne.Response, Any] = baseEndpoint
    .get
    .description("Get particular location given by id")
    .in(idPath)
    .out(jsonBody[readOne.Response])

  private val updateEndpoint: PublicEndpoint[update.Request, StatusCode, update.Response, Any] = baseEndpoint
    .put
    .description("Update longitude and latitude of given location in batch")
    .in(jsonBody[update.Request])
    .out(jsonBody[update.Response])

  private val updateOneEndpoint: PublicEndpoint[updateOne.Request, StatusCode, updateOne.Response, Any] = baseEndpoint
    .put
    .description("Update longitude and latitude of particular location")
    .in(idPath)
    .in(jsonBody[updateOne.RequestBody])
    .out(jsonBody[updateOne.Response])

  private val deleteEndpoint: PublicEndpoint[delete.Request, StatusCode, delete.Response, Any] = baseEndpoint
    .delete
    .description("Delete list of given locations in batch")
    .in(idsQuery)
    .out(statusCode(StatusCode.NoContent))

  private val deleteOneEndpoint: PublicEndpoint[deleteOne.Request, StatusCode, deleteOne.Response, Any] = baseEndpoint
    .delete
    .description("Delete particular location given by id")
    .in(idPath)
    .out(statusCode(StatusCode.NoContent))


  
  private val allEnpoints =
    List(
      createEndpoint,
      createOneEndpoint,
      readEndpoint,
      readOneEndpoint,
      updateEndpoint,
      updateOneEndpoint,
      deleteEndpoint,
      deleteOneEndpoint
    )
package com.vportnov.locations.api

import cats.effect.IO
import org.http4s.HttpRoutes
import sttp.tapir._
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

  def getReadRoute(logic: (read.Request) => IO[read.Response]): HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(readEndpoint.serverLogicSuccess(logic))

  def swaggerUIRoutes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(SwaggerInterpreter().fromEndpoints[IO](allEnpoints, "Locations Service", "1.0.0"))

  private val periodQuery: EndpointInput[PeriodQuery] =
    query[OptionalDateTime]("from")
    .and(query[OptionalDateTime]("to"))
    .mapTo[PeriodQuery]

  // type OptionalStrings = Option[Seq[String]]
  // case class IdsQuery(id: OptionalStrings)
  // val idsQuery: EndpointInput[IdsQuery] = query[OptionalStrings]("ids").mapTo[IdsQuery]

  private val baseEndpoint = endpoint
    .in("api" / "v1.0")

  private val createEndpoint: PublicEndpoint[create.Request, String, create.Response, Any] = baseEndpoint
    .post
    .in("locations")
    .in(jsonBody[create.Request])
    .errorOut(stringBody)
    .out(jsonBody[create.Response])

  private val readEndpoint: PublicEndpoint[read.Request, String, read.Response, Any] = baseEndpoint
    .get
    .in("locations")
    .in(periodQuery)
    .errorOut(stringBody)
    .out(jsonBody[read.Response])
  
  private val allEnpoints = List(createEndpoint, readEndpoint)
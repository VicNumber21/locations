package com.vportnov.locations.api

import cats.effect.Async
import cats.syntax.all._

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes

import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.model.StatusCode
import sttp.tapir.json.circe._
import io.circe.syntax._

import sttp.capabilities.fs2.Fs2Streams
import fs2.Stream

import com.vportnov.locations.model
import com.vportnov.locations.api.types.{ field, request, response, given }
import com.vportnov.locations.api.tapir.fs2stream.json._
import com.vportnov.locations.utils.fs2stream.syntax._


final class LocationsRoutes[F[_]: Async](storage: model.StorageExt[F]) extends Http4sDsl[F]:
  val createRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.createEndpoint
        .serverLogicSuccess(request => reply(storage.createLocations(request.toModel), response.Location.from))
    )
  
  val createOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.createOneEndpoint
        .serverLogic(request => reply(storage.createLocation(request.toModel), response.Location.from, conflictError))
    )

  val getRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.getEndpoint
        .serverLogicSuccess(request => reply(storage.getLocations(request.period, request.ids), response.Location.from))
    )

  val getOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.getOneEndpoint
        .serverLogic(id => reply(storage.getLocation(id), response.Location.from, notFoundError))
    )

  val updateRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.updateEndpoint
        .serverLogicSuccess(request => reply(storage.updateLocations(request.toModel), response.Location.from))
    )
  
  val updateOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.updateOneEndpoint
        .serverLogic(request => reply(storage.updateLocation(request.toModel), response.Location.from, notFoundError))
    )

  val deleteRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.deleteEndpoint
        .serverLogic(ids => reply(storage.deleteLocations(ids), deleteSuccess, commonErrors))
    )

  val deleteOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.deleteOneEndpoint
        .serverLogic(id => reply(storage.deleteLocation(id), deleteSuccess, commonErrors))
    )
  
  val statsRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.statsEndpoint
        .serverLogicSuccess(period => reply(storage.locationStats(period), response.Stats.from))
    )

  private def reply[SR, O](stream: Stream[F, SR], mapper: SR => O)(using encoder: io.circe.Encoder[O]): F[Stream[F, Byte]] =
    stream
      .map(mapper)
      .map(_.asJson.noSpaces)
      .catchError { error =>
        response.ServerError(error.getMessage()).asJson.noSpaces
      }
      .toJsonArray
      .through(fs2.text.utf8.encode)
      .pure[F]

  private def reply[SR, O](storageResponse: F[SR], mapper: SR => O, errorMapper: Throwable => StatusCode): F[Either[StatusCode, O]] =
    for
      unpacked <- storageResponse.attempt
    yield unpacked.map(mapper).left.map(errorMapper)

  private def deleteSuccess(count: Int): LocationsRoutes.ResponseCode = count match
    case 0 => LocationsRoutes.ResponseCode.Ok
    case n if n > 0 => LocationsRoutes.ResponseCode.NoContent
    case strange => LocationsRoutes.ResponseCode.InternalServerError
  
  private def notFoundError(error: Throwable) = error match
    case _ : java.util.NoSuchElementException => StatusCode.NotFound
    case _ => commonErrors(error)

  private def conflictError(error: Throwable) = error match
    case _ : java.util.NoSuchElementException => StatusCode.Conflict
    case _ => commonErrors(error)

  private def commonErrors(error: Throwable) =
    error match
      case _: IllegalArgumentException => StatusCode.BadRequest
      case _ => StatusCode.InternalServerError
  
  val routes =
    createRoute <+>
    createOneRoute <+>
    getRoute <+>
    getOneRoute <+>
    updateRoute <+>
    updateOneRoute <+>
    deleteRoute <+>
    deleteOneRoute <+>
    statsRoute

object LocationsRoutes:
  val periodQuery: EndpointInput[model.Period] =
    query[model.Location.OptionalTimestamp]("from")
    .and(query[model.Location.OptionalTimestamp]("to"))
    .mapTo[model.Period]

  val idsQuery: EndpointInput[model.Location.Ids] =
    query[model.Location.Ids]("id")
      .validateIterable(field.Id.meta.underlyingValidator)
  
  // TODO move path and query to fileds?
  def idPath: EndpointInput[field.Id.Underlying] =
    path[field.Id.Underlying]("id")
      .validate(field.Id.meta.underlyingValidator)

  val baseEndpoint = endpoint
    .in("api" / "v1.0" / "locations")
    .errorOut(statusCode) // TODO add json error as ServerError

  def createEndpoint[F[_]]: PublicEndpoint[request.Create, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .post
    .description("Create locations in batch.")
    .tag("Create")
    .in(request.Create.body)
    .out(fs2StreamJsonBodyUTF8[F, List[response.Location]])
    .out(statusCode(StatusCode.Created))

  val idPathAndCreateOneBody = idPath.and(request.CreateOne.body).mapTo[request.CreateOne]

  val createOneEndpoint: PublicEndpoint[request.CreateOne, StatusCode, response.Location, Any] = baseEndpoint
    .post
    .description("Create a single location.")
    .tag("Create")
    .in(idPathAndCreateOneBody)
    .out(jsonBody[response.Location])
    .out(statusCode(StatusCode.Created))

  val periodAndIdsQuery = periodQuery.and(idsQuery).mapTo[request.Get]
  
  def getEndpoint[F[_]]: PublicEndpoint[request.Get, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .get
    .description("Get list of locations: all, particular ids or created before or after or between dates.")
    .tag("Get")
    .in(periodAndIdsQuery)
    .out(fs2StreamJsonBodyUTF8[F, List[response.Location]])

  val getOneEndpoint: PublicEndpoint[model.Location.Id, StatusCode, response.Location, Any] = baseEndpoint
    .get
    .description("Get particular location by given id.")
    .tag("Get")
    .in(idPath) // TODO make it request.Get.input here and similar in other endpoints?
    .out(jsonBody[response.Location])

  def updateEndpoint[F[_]]: PublicEndpoint[request.Update, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .put
    .description("Update longitude and latitude of given location in batch.")
    .tag("Update")
    .in(request.Update.body)
    .out(fs2StreamJsonBodyUTF8[F, List[response.Location]])

  val idPathAndUpdateOneBody = idPath.and(request.UpdateOne.body).mapTo[request.UpdateOne]

  val updateOneEndpoint: PublicEndpoint[request.UpdateOne, StatusCode, response.Location, Any] = baseEndpoint
    .put
    .description("Update longitude and latitude of particular location.")
    .tag("Update")
    .in(idPathAndUpdateOneBody)
    .out(jsonBody[response.Location])

  val deleteEndpoint: PublicEndpoint[model.Location.Ids, StatusCode, ResponseCode, Any] = baseEndpoint
    .delete
    .description("Delete list of given locations in batch.")
    .tag("Delete")
    .in(idsQuery)
    .out(deleteSuccessStatusCodes)

  val deleteOneEndpoint: PublicEndpoint[model.Location.Id, StatusCode, ResponseCode, Any] = baseEndpoint
    .delete
    .description("Delete particular location by given id.")
    .tag("Delete")
    .in(idPath)
    .out(deleteSuccessStatusCodes)

  def statsEndpoint[F[_]]: PublicEndpoint[model.Period, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .get
    .description("Get statistic about count of created locations per day." ++
                 " Statistic could be requested for all locations or created before or after or between dates.")
    .tag("Statistics")
    .in("-" / "stats")
    .in(periodQuery)
    .out(fs2StreamJsonBodyUTF8[F, List[response.Stats]])
  
  // TODO implement success / error status codes like this
  // TODO it does not work as expected, it does not show code returned from serverLogic
  // https://tapir.softwaremill.com/en/latest/endpoint/oneof.html?highlight=oneOf#oneof-outputs
  // IT DOES NOT WORK VIA ENUM, but works as below
  // Most probably it makes sense to implement subtypes of ServerError (ServerResponse better?) and pass them as JSON on
  // particular error as well as map the class to needed status code
  // It also looks like response.Location must be derived from the same thing to be able to map 200 on it
  sealed trait ResponseCode
  object ResponseCode:
    case object Ok extends ResponseCode
    case object NoContent extends ResponseCode
    case object InternalServerError extends ResponseCode

  val test = baseEndpoint.out(deleteSuccessStatusCodes)

  def deleteSuccessStatusCodes =
    oneOf[ResponseCode](
      oneOfVariant(statusCode(StatusCode.NoContent).and(emptyOutputAs(ResponseCode.NoContent).description("Deleted successfuly."))),
      oneOfVariant(statusCode(StatusCode.Ok).and(emptyOutputAs(ResponseCode.Ok).description("Not found so already removed."))),
      oneOfDefaultVariant(statusCode(StatusCode.InternalServerError).and(emptyOutputAs(ResponseCode.InternalServerError).description("Generic server error.")))
    )
  
  val endpoints: List[AnyEndpoint] =
    List(
      createEndpoint,
      createOneEndpoint,
      getEndpoint,
      getOneEndpoint,
      updateEndpoint,
      updateOneEndpoint,
      deleteEndpoint,
      deleteOneEndpoint,
      statsEndpoint
    )

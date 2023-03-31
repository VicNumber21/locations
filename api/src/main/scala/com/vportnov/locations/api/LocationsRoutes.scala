package com.vportnov.locations.api

import cats.effect.Async
import cats.syntax.all._

import org.http4s.dsl.Http4sDsl
import org.http4s.HttpRoutes

import sttp.tapir._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.model.StatusCode
import io.circe.syntax._

import sttp.capabilities.fs2.Fs2Streams
import fs2.Stream

import com.vportnov.locations.model.StorageExt
import com.vportnov.locations.api.types.{ request, response }
import com.vportnov.locations.utils.fs2stream.syntax._
import com.vportnov.locations.utils. { LoggingIO, ServerError }


final class LocationsRoutes[F[_]: Async](storage: StorageExt[F]) extends Http4sDsl[F] with LoggingIO[F]:
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
        .serverLogicSuccess(request => reply(storage.getLocations(request.period.toModel, request.ids.v), response.Location.from))
    )

  val getOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.getOneEndpoint
        .serverLogic(request => reply(storage.getLocation(request.v), response.Location.from, notFoundError))
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
        .serverLogic(request => reply(storage.deleteLocations(request.v), deleteSuccess, commonErrors))
    )

  val deleteOneRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.deleteOneEndpoint
        .serverLogic(request => reply(storage.deleteLocation(request.v), deleteSuccess, commonErrors))
    )
  
  val statsRoute: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      LocationsRoutes.statsEndpoint
        .serverLogicSuccess(request => reply(storage.locationStats(request.toModel), response.Stats.from))
    )

  private def reply[SR, O](stream: Stream[F, SR], mapper: SR => O)(using encoder: io.circe.Encoder[O]): F[Stream[F, Byte]] =
    stream
      .map(mapper)
      .map(_.asJson.noSpaces)
      .logWhenDone
      .catchError { error =>
        response.Status.InternalServerError(error.getMessage()).asJson.noSpaces
      }
      .toJsonArray
      .through(fs2.text.utf8.encode)
      .pure[F]
      .logWhenDone

  private def reply[SR, O](storageResponse: F[SR], mapper: SR => O, errorMapper: Throwable => StatusCode): F[Either[StatusCode, O]] =
    val result = for
      unpacked <- storageResponse
        .map(mapper)
        .logWhenDone
        .attempt
    yield unpacked.left.map(errorMapper)
    result.logWhenDone

  private def deleteSuccess(count: Int): response.Delete = count match
    case 0 => response.Status.Ok()
    case n if n > 0 => response.Status.NoContent()
    case strange => throw ServerError.Internal(s"Count could not be less than 0 (got ${strange})")
  
  private def notFoundError(error: Throwable) =
    ServerError.fromCause(error).kind match
      case ServerError.Kind.NoSuchElement => StatusCode.NotFound
      case _ => commonErrors(error)

  private def conflictError(error: Throwable) =
    ServerError.fromCause(error).kind match
      case ServerError.Kind.NoSuchElement => StatusCode.Conflict
      case _ => commonErrors(error)

  private def commonErrors(error: Throwable) =
    ServerError.fromCause(error).kind match
      case ServerError.Kind.IllegalArgument => StatusCode.BadRequest
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
  val baseEndpoint = endpoint
    .in("api" / "v1.0" / "locations")
    .errorOut(statusCode) // TODO add json error as Status

  def createEndpoint[F[_]]: PublicEndpoint[request.Create, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .post
    .description("Create locations in batch.")
    .tag("Create")
    .in(request.Create.input)
    .out(response.Location.body.stream)
    .out(statusCode(StatusCode.Created))

  val createOneEndpoint: PublicEndpoint[request.CreateOne, StatusCode, response.Location, Any] = baseEndpoint
    .post
    .description("Create a single location.")
    .tag("Create")
    .in(request.CreateOne.input)
    .out(response.Location.body.json)
    .out(statusCode(StatusCode.Created))

  def getEndpoint[F[_]]: PublicEndpoint[request.Get, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .get
    .description("Get list of locations: all, particular ids or created before or after or between dates.")
    .tag("Get")
    .in(request.Get.input)
    .out(response.Location.body.stream)

  val getOneEndpoint: PublicEndpoint[request.GetOne, StatusCode, response.Location, Any] = baseEndpoint
    .get
    .description("Get particular location by given id.")
    .tag("Get")
    .in(request.GetOne.input)
    .out(response.Location.body.json)

  def updateEndpoint[F[_]]: PublicEndpoint[request.Update, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .put
    .description("Update longitude and latitude of given location in batch.")
    .tag("Update")
    .in(request.Update.input)
    .out(response.Location.body.stream)

  val updateOneEndpoint: PublicEndpoint[request.UpdateOne, StatusCode, response.Location, Any] = baseEndpoint
    .put
    .description("Update longitude and latitude of particular location.")
    .tag("Update")
    .in(request.UpdateOne.input)
    .out(response.Location.body.json)

  val deleteEndpoint: PublicEndpoint[request.Delete, StatusCode, response.Delete, Any] = baseEndpoint
    .delete
    .description("Delete list of given locations in batch.")
    .tag("Delete")
    .in(request.Delete.input)
    .out(response.Delete.output)

  val deleteOneEndpoint: PublicEndpoint[request.DeleteOne, StatusCode, response.Delete, Any] = baseEndpoint
    .delete
    .description("Delete particular location by given id.")
    .tag("Delete")
    .in(request.DeleteOne.input)
    .out(response.Delete.output)

  def statsEndpoint[F[_]]: PublicEndpoint[request.Stats, StatusCode, Stream[F, Byte], Fs2Streams[F]] = baseEndpoint
    .get
    .description("Get statistic about count of created locations per day." ++
                 " Statistic could be requested for all locations or created before or after or between dates.")
    .tag("Statistics")
    .in("-" / "stats")
    .in(request.Stats.input)
    .out(response.Stats.body.stream)
  
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

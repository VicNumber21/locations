package com.vportnov.locations.api

import cats.effect.Async
import cats.syntax.all._

import org.http4s.HttpRoutes

import sttp.tapir._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import sttp.model.StatusCode
import io.circe.syntax._

import sttp.capabilities.fs2.Fs2Streams
import fs2.Stream

import com.vportnov.locations.model.StorageExt
import com.vportnov.locations.api.types.{ request, response, field }
import com.vportnov.locations.utils.fs2stream.syntax._
import com.vportnov.locations.utils. { LoggingIO, ServerError }


final class HttpEndpoints[F[_]: Async](storage: StorageExt[F]) extends  LoggingIO[F]:
  val serverEndpoints: List[ServerEndpoint[Fs2Streams[F], F]] =
    List(
      create,
      createOne,
      get,
      getOne,
      update,
      updateOne,
      delete,
      deleteOne,
      stats
    )
  
  def swaggerEndpoints(title: String, version: String): List[ServerEndpoint[Fs2Streams[F], F]] =
    SwaggerInterpreter().fromEndpoints[F](serverEndpoints.map(_.endpoint), title, version)
  
  private def reply[SR, O](stream: Stream[F, SR], mapper: SR => O, errorMapper: Throwable => response.Status.Error)
                          (using encoder: io.circe.Encoder[O]): F[Stream[F, Byte]] =
    stream
      .map(mapper)
      .map(_.asJson.noSpaces)
      .logError
      .recover { error =>
        errorMapper(error).toJson.noSpaces
      }
      .toJsonArray
      .through(fs2.text.utf8.encode)
      .logWhenDone
      .pure[F]

  private def reply[SR, O](storageResponse: F[SR], mapper: SR => O, errorMapper: Throwable => response.Status): F[Either[response.Status, O]] =
    for
      unpacked <- storageResponse
        .map(mapper)
        .logError
        .attempt
    yield unpacked.left.map(errorMapper)

  private def deleteSuccess(count: Int): response.Status = count match
    case 0 => response.Status.Ok()
    case n if n > 0 => response.Status.NoContent()
    case strange => throw ServerError.Internal(s"Count could not be less than 0 (got ${strange})")
  
  private def notFoundError(id: field.Id): Throwable => response.Status.Error =
    (error: Throwable) =>
      val se = ServerError.fromCause(error)
      se.kind match
        case ServerError.Kind.NoSuchElement => response.Status.NotFound(id, se.uuid)
        case _ => commonErrors(error)

  private def conflictError(id: field.Id): Throwable => response.Status.Error =
    (error: Throwable) =>
      val se = ServerError.fromCause(error)
      se.kind match
        case ServerError.Kind.NoSuchElement => response.Status.Conflict(id, se.uuid)
        case _ => commonErrors(error)

  private def commonErrors(error: Throwable): response.Status.Error =
    val se = ServerError.fromCause(error)
    se.kind match
      case ServerError.Kind.IllegalArgument => response.Status.BadRequest(se.message, se.uuid)
      case _ => response.Status.InternalServerError(se.uuid)

  private def baseEndpoint = endpoint
    .in("api" / "v1.0" / "locations")
  
  private def create: ServerEndpoint[Fs2Streams[F], F] = baseEndpoint
    .post
    .description(HttpEndpoints.meta.description.create)
    .tag(HttpEndpoints.meta.tag.create)
    .in(request.Create.input)
    .errorOut(response.Create.error)
    .out(response.Create.output)
    .serverLogicSuccess(request => reply(storage.createLocations(request.toModel), response.Location.from, commonErrors))
  
  private def createOne: ServerEndpoint[Any, F] = baseEndpoint
    .post
    .description(HttpEndpoints.meta.description.createOne)
    .tag(HttpEndpoints.meta.tag.create)
    .in(request.CreateOne.input)
    .errorOut(response.CreateOne.error)
    .out(response.CreateOne.output)
    .serverLogic(request => reply(storage.createLocation(request.toModel), response.Location.from, conflictError(request.id)))

  private def get: ServerEndpoint[Fs2Streams[F], F] = baseEndpoint
    .get
    .description(HttpEndpoints.meta.description.get)
    .tag(HttpEndpoints.meta.tag.get)
    .in(request.Get.input)
    .errorOut(response.Get.error)
    .out(response.Get.output)
    .serverLogicSuccess(request => reply(storage.getLocations(request.period.toModel, request.ids.v), response.Location.from, commonErrors))

  private def getOne: ServerEndpoint[Any, F] = baseEndpoint
    .get
    .description(HttpEndpoints.meta.description.getOne)
    .tag(HttpEndpoints.meta.tag.get)
    .in(request.GetOne.input)
    .errorOut(response.GetOne.error)
    .out(response.GetOne.output)
    .serverLogic(request => reply(storage.getLocation(request.v), response.Location.from, notFoundError(request)))

  private def update: ServerEndpoint[Fs2Streams[F], F] = baseEndpoint
    .put
    .description(HttpEndpoints.meta.description.update)
    .tag(HttpEndpoints.meta.tag.update)
    .in(request.Update.input)
    .errorOut(response.Update.error)
    .out(response.Update.output)
    .serverLogicSuccess(request => reply(storage.updateLocations(request.toModel), response.Location.from, commonErrors))
  
  private def updateOne: ServerEndpoint[Any, F] = baseEndpoint
    .put
    .description(HttpEndpoints.meta.description.updateOne)
    .tag(HttpEndpoints.meta.tag.update)
    .in(request.UpdateOne.input)
    .errorOut(response.UpdateOne.error)
    .out(response.UpdateOne.output)
    .serverLogic(request => reply(storage.updateLocation(request.toModel), response.Location.from, notFoundError(request.id)))

  private def delete: ServerEndpoint[Fs2Streams[F], F] = baseEndpoint
    .delete
    .description(HttpEndpoints.meta.description.delete)
    .tag(HttpEndpoints.meta.tag.delete)
    .in(request.Delete.input)
    .errorOut(response.Delete.error)
    .out(response.Delete.output)
    .serverLogic(request => reply(storage.deleteLocations(request.v), deleteSuccess, commonErrors))

  private def deleteOne: ServerEndpoint[Any, F] = baseEndpoint
    .delete
    .description(HttpEndpoints.meta.description.deleteOne)
    .tag(HttpEndpoints.meta.tag.delete)
    .in(request.DeleteOne.input)
    .errorOut(response.DeleteOne.error)
    .out(response.DeleteOne.output)
    .serverLogic(request => reply(storage.deleteLocation(request.v), deleteSuccess, commonErrors))
  
  private def stats: ServerEndpoint[Fs2Streams[F], F] = baseEndpoint
    .get
    .description(HttpEndpoints.meta.description.stats)
    .tag(HttpEndpoints.meta.tag.stats)
    .in("-" / "stats")
    .in(request.Stats.input)
    .errorOut(response.Stats.error)
    .out(response.Stats.output)
    .serverLogicSuccess(request => reply(storage.locationStats(request.toModel), response.Stats.from, commonErrors))


object HttpEndpoints:
  object meta:
    object tag:
      val create = "Create"
      val get = "Get"
      val update = "Update"
      val delete = "Delete"
      val stats = "Statistics"

    object description:
      val create = "Create locations in batch."
      val createOne = "Create a single location."
      val get = """|Get list of locations using the following options:
                   |
                   |  - all (default)
                   |
                   |  - by particular ids (one ore more ids provided)
                   |
                   |  - created before given timestamp (to)
                   |
                   |  - created after given timestamp (from)
                   |
                   |  - created between given timestamps (from and to)
                   |
                   |UTC is used for timestamps.
                   |""".stripMargin
      val getOne = "Get particular location by given id."
      val update = "Update longitude and latitude of given locations in batch."
      val updateOne = "Update longitude and latitude of particular location."
      val delete = "Delete list of locations with given ids in batch."
      val deleteOne = "Delete particular location by given id."
      val stats = """|Get statistic about count of created locations per day.
                     |
                     |UTC is used in for dates in request and response.
                     |
                     |If request date contains time, it is automatically rounded to the beginning of the day in UTC.
                     |""".stripMargin

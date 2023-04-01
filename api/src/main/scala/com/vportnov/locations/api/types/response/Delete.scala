package com.vportnov.locations.api.types.response

import sttp.tapir._

  // TODO implement success / error status codes like this
  // https://tapir.softwaremill.com/en/latest/endpoint/oneof.html?highlight=oneOf#oneof-outputs
  // IT DOES NOT WORK VIA ENUM, but works as below
  // Most probably it makes sense to implement subtypes of ServerError (ServerResponse better?) and pass them as JSON on
  // particular error as well as map the class to needed status code
  // It also looks like response.Location must be derived from the same thing to be able to map 200 on it
type Delete = Status
object Delete:
  def output =
    oneOf[Status](
      oneOfVariant(Status.NoContent.asStatusCodeWithEmptyBody("Deleted successfuly.")),
      oneOfVariant(Status.Ok.asStatusCodeWithEmptyBody("Not found so already removed."))
    )
  
  def error =
    oneOf[Status](
      oneOfVariant(Status.BadRequest.asStatusCodeWithJsonBody),
      oneOfDefaultVariant(Status.InternalServerError.asStatusCodeWithJsonBody)
    )

package com.vportnov.locations.api.types.response

import sttp.tapir._
import sttp.model.StatusCode


object UpdateOne:
  def output = statusCode(StatusCode.Ok).and(Location.body.json)
  
  def error =
    oneOf[Status](
      oneOfVariant(Status.BadRequest.asStatusCodeWithJsonBody),
      oneOfVariant(Status.NotFound.asStatusCodeWithJsonBody),
      oneOfDefaultVariant(Status.InternalServerError.asStatusCodeWithJsonBody)
    )

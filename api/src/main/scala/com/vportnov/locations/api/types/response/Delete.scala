package com.vportnov.locations.api.types.response

import sttp.tapir._


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

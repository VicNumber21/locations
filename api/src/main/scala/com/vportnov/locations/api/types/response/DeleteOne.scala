package com.vportnov.locations.api.types.response

import sttp.tapir._


object DeleteOne:
  def output =
    oneOf[Status](
      oneOfVariant(Status.NoContent.asStatusCodeWithEmptyBody(meta.description.NoContent)),
      oneOfVariant(Status.Ok.asStatusCodeWithEmptyBody(meta.description.Ok))
    )
  
  def error =
    oneOf[Status](
      oneOfVariant(Status.BadRequest.asStatusCodeWithJsonBody),
      oneOfDefaultVariant(Status.InternalServerError.asStatusCodeWithJsonBody)
    )

  object meta:
    object description:
      val Ok = "No location with given id was found but it is treated as 'already removed'."
      val NoContent = "The location was found and removed."
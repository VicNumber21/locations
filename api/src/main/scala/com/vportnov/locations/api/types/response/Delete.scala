package com.vportnov.locations.api.types.response

import sttp.tapir._


object Delete:
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
      val Ok = "No location with requested ids were found but it is treated as 'already removed'."
      val NoContent = "At least one of locations was found and removed."
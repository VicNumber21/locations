package com.vportnov.locations.api.types.request


import com.vportnov.locations.model

final case class Get(period: model.Period, ids: model.Location.Ids)

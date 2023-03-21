package com.vportnov.locations.model

import cats.effect.Sync
import fs2.Stream

import com.vportnov.locations.model.StreamExtOps._


trait StorageExt[F[_]: Sync] extends Storage[F]:

  def createLocation(location: Location.WithOptionalCreatedField): F[Location.WithCreatedField] = 
    createLocations(List(location)).firstEntry

  def getLocation(id: Location.Id): F[Location.WithCreatedField] = 
    getLocations(Period(None, None), List(id)).firstEntry

  def updateLocation(location: Location.WithoutCreatedField): F[Location.WithCreatedField] = 
    updateLocations(List(location)).firstEntry

  def deleteLocation(id: Location.Id): F[Either[Throwable, Int]] =
    deleteLocations(List(id))
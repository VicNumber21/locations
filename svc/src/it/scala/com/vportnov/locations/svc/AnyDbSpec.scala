package com.vportnov.locations.svc

import org.scalatest.{ BeforeAndAfterAllConfigMap, GivenWhenThen, ConfigMap }
import org.scalatest.flatspec.AnyFlatSpec


trait AnyDbSpec extends AnyFlatSpec with BeforeAndAfterAllConfigMap with GivenWhenThen:
  val db = DbContainer
  db.register

  override protected def beforeAll(configMap: ConfigMap): Unit =
    db.onSuiteBegin(configMap.get("fastRun").isDefined)

  override protected def afterAll(configMap: ConfigMap): Unit =
    db.onSuiteEnd(configMap.get("fastRun").isDefined)

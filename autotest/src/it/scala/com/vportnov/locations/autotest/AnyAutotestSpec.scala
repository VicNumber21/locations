package com.vportnov.locations.autotest

import org.scalatest.{ BeforeAndAfterAllConfigMap, GivenWhenThen, ConfigMap }
import org.scalatest.flatspec.AnyFlatSpec


trait AnyAutotestSpec extends AnyFlatSpec with BeforeAndAfterAllConfigMap with GivenWhenThen:
  val apps = SolutionContainers
  apps.register

  override protected def beforeAll(configMap: ConfigMap): Unit =
    apps.onSuiteBegin(configMap.get("fastRun").isDefined)

  override protected def afterAll(configMap: ConfigMap): Unit =
    apps.onSuiteEnd(configMap.get("fastRun").isDefined)

package com.vportnov.locations.autotest

import com.dimafeng.testcontainers._
import org.testcontainers.containers.wait.strategy.Wait

import scala.util.Random
import java.io.File

import com.vportnov.locations.api
import com.vportnov.locations.svc


object SolutionContainers:
  def register: Unit = count += 1
  def isRunning: Boolean = container.isDefined
  def dbConfig: svc.Config.Database = getAppsConfig.db
  def apiPort: Int = getAppsConfig.apiPort

  def onSuiteBegin(isFastRun: Boolean): Unit =
    generateConfig(isFastRun)
    if count > 0 then startContainer()

  def onSuiteEnd(isFastRun: Boolean): Unit =
    count -= 1
    if count <= 0 then stopContainer()


  private def appsEnv: Map[String, String] = Map(
      "API_PORT" -> s"${getAppsConfig.apiPort}",
      "DB_ADMIN" -> dbConfig.admin.login,
      "DB_ADMIN_PASSWORD" -> dbConfig.admin.password,
      "DB_VOLUME" -> getAppsConfig.volume,
      "DB_NAME" -> "locations",
      "DB_PORT" -> s"${getAppsConfig.dbPort}"
    )

  private def containerDef =
    DockerComposeContainer.Def(
      composeFiles = new File("./docker/compose.yaml"),
      env = appsEnv,
      waitingFor = Some(WaitingForService("svc_1", Wait.forLogMessage(".*grpc server started on.*", getAppsConfig.waitTimes)))
    )

  private final class AppsConfig(isFastRun: Boolean):
    val apiPort = 58080

    val voumeBasePath = "/tmp/locations_auto_test"
    val volume =
      if isFastRun
        then s"${voumeBasePath}/permanent"
        else s"${voumeBasePath}/${Random.alphanumeric.take(10).mkString}"
    val dbPort = 65432
    val waitTimes = 1
    val db = svc.Config.Database(
      driver = "org.postgresql.Driver",
      url = s"jdbc:postgresql://localhost:${dbPort}",
      name = "locations",
      user = svc.Config.Credentials("locator", "locator"),
      admin = svc.Config.Credentials("admin", "admin")
    )

  private def generateConfig(isFastRun: Boolean): Unit =
    if (appsConfig.isEmpty) then appsConfig = Some(new AppsConfig(isFastRun))
  
  private def getAppsConfig: AppsConfig = appsConfig.get

  private def startContainer(): Unit =
    if !isRunning then container = Some(containerDef.start())
  
  private def stopContainer(): Unit =
    if isRunning then
      container.get.stop()
      container.get.close()

  @volatile private var appsConfig: Option[AppsConfig] = None;
  @volatile private var container: Option[Container] = None;
  @volatile private var count: Int = 0;

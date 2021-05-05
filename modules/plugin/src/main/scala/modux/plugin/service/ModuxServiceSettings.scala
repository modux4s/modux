package modux.plugin.service

import modux.plugin.core.ModuxState
import modux.shared.ServerDecl
import sbt.{SettingKey, TaskKey, settingKey, taskKey}

trait ModuxServiceSettings {

  lazy val MODUX_MODE: String = "modux.mode"
  lazy val MODE_COMPILE: String = "compile"
  lazy val MODE_EXPORT: String = "export"

  lazy val contact = settingKey[Option[String]]("Project contact for swagger export")
  lazy val servers = settingKey[Seq[ServerDecl]]("Endpoints for swagger export")
  lazy val startServer: TaskKey[Unit] = taskKey("Starts modux server")
  lazy val moduxState: TaskKey[ModuxState] = taskKey("Server reloader builder")
  lazy val moduxHost = settingKey[String]("host")
  lazy val moduxPort = settingKey[Int]("port")
  lazy val moduxLogFile = settingKey[String]("logger.file")
  lazy val moduxExportYaml: TaskKey[Unit] = taskKey("Exports service to swagger yaml format.")
  lazy val moduxExportJson: TaskKey[Unit] = taskKey("Exports service to swagger json format.")
  lazy val moduxOpenAPIVersion: SettingKey[Int] = settingKey[Int]("Open API version. Possibles values: 2 and 3.")
  //************** modules **************//

  lazy val moduxStopHook: TaskKey[Unit] = taskKey[Unit]("Implements stop hooks")

}

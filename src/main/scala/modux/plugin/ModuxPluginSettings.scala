package modux.plugin

import modux.plugin.core.{ModuxState, ServerReloader}
import modux.shared.ServerDecl
import sbt._

trait ModuxPluginSettings {

  lazy val MODUX_MODE: String = "modux.mode"
  lazy val MODE_COMPILE: String = "compile"
  lazy val MODE_EXPORT: String = "export"

  lazy val contact = settingKey[Option[String]]("Project contact for swagger export")
  lazy val servers = settingKey[Seq[ServerDecl]]("Endpoints for swagger export")

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

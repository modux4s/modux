package modux.plugin

import modux.plugin.core.{ModuxState, ServerReloader}
import modux.shared.ServerDecl
import sbt._

trait ModuxPluginSettings {

  lazy val MODUX_MODE: String = "modux.mode"
  lazy val MODE_COMPILE: String = "compile"
  lazy val MODE_EXPORT: String = "export"

  lazy val contact = settingKey[Option[String]]("Project contact for swagger export")
  lazy val licenseName = settingKey[Option[String]]("License name for swagger export")
  lazy val licenseUrl = settingKey[Option[String]]("License url for swagger export")
  lazy val servers = settingKey[Seq[ServerDecl]]("Endpoints for swagger export")

  lazy val moduxState: TaskKey[ModuxState] = taskKey("Server reloader builder")
  lazy val moduxAppName = settingKey[String]("host")
  lazy val moduxHost = settingKey[String]("host")
  lazy val moduxPort = settingKey[Int]("port")
  lazy val moduxLogFile = settingKey[String]("logger.file")
  lazy val moduxHotReloadAttr: AttributeKey[ServerReloader] = AttributeKey[ServerReloader]("server-reloader", "MServer reloader service")
  lazy val moduxExportYaml: TaskKey[Unit] = taskKey("Exports service to swagger yaml format.")
  lazy val moduxExportJson: TaskKey[Unit] = taskKey("Exports service to swagger json format.")
  //************** modules **************//
  lazy val moduxVersion: String = "0.1.0-SNAPSHOT"
  lazy val moduxCore: ModuleID = "jsoft.modux" %% "modux-core" % moduxVersion
  lazy val moduxSerialization: ModuleID = "jsoft.modux" %% "modux-serialization" % moduxVersion
}

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
  lazy val moduxOpenAPIVersion  = settingKey[Int]("Open API version. Possibles values: 2 and 3.")
  //************** modules **************//
  lazy val moduxVersion: String = "1.0.11"
  lazy val moduxMacros: ModuleID = "jsoft.modux" %% "modux-macros" % moduxVersion
  lazy val moduxCore: ModuleID = "jsoft.modux" %% "modux-core" % moduxVersion
  lazy val moduxServer: ModuleID = "jsoft.modux" %% "modux-server" % moduxVersion
  lazy val moduxOpenAPIV2: ModuleID = "jsoft.modux" %% "modux-swagger-v2" % moduxVersion
  lazy val moduxOpenAPIV3: ModuleID = "jsoft.modux" %% "modux-swagger-v3" % moduxVersion
  lazy val moduxSerialization: ModuleID = "jsoft.modux" %% "modux-serialization" % moduxVersion
}

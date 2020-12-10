package modux.plugin

import sbt._

object ModuxPluginDefaults {
  lazy val moduxVersion: String = "1.1.0-SNAPSHOT"
  lazy val moduxMacros: ModuleID = "jsoft.modux" %% "modux-macros" % moduxVersion
  lazy val moduxCore: ModuleID = "jsoft.modux" %% "modux-core" % moduxVersion
  lazy val moduxServer: ModuleID = "jsoft.modux" %% "modux-server" % moduxVersion
  lazy val moduxOpenAPIV2: ModuleID = "jsoft.modux" %% "modux-swagger-v2" % moduxVersion
  lazy val moduxOpenAPIV3: ModuleID = "jsoft.modux" %% "modux-swagger-v3" % moduxVersion
  lazy val moduxSerialization: ModuleID = "jsoft.modux" %% "modux-serialization" % moduxVersion
  lazy val moduxKafka: ModuleID = "jsoft.modux" %% "modux-kafka-core" % moduxVersion
}

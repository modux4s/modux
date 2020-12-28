package modux.plugin

import sbt._

object ModuxPluginDefaults {
  val pac4jVersion: String = "4.3.0"
  val moduxVersion: String = "1.2.2-SNAPSHOT"
  val moduxMacros: ModuleID = "jsoft.modux" %% "modux-macros" % moduxVersion
  val moduxCore: ModuleID = "jsoft.modux" %% "modux-core" % moduxVersion
  val moduxServer: ModuleID = "jsoft.modux" %% "modux-server" % moduxVersion
  val moduxOpenAPIV2: ModuleID = "jsoft.modux" %% "modux-swagger-v2" % moduxVersion
  val moduxOpenAPIV3: ModuleID = "jsoft.modux" %% "modux-swagger-v3" % moduxVersion
  val moduxSerialization: ModuleID = "jsoft.modux" %% "modux-serialization" % moduxVersion
  val moduxKafka: ModuleID = "jsoft.modux" %% "modux-kafka-core" % moduxVersion

  private val pac4j = "org.pac4j" % "pac4j-core" % pac4jVersion
  private val pac4jOAuth = "org.pac4j" % "pac4j-oauth" % pac4jVersion

  val webSecurityDeps = Seq(pac4j, pac4jOAuth)
}

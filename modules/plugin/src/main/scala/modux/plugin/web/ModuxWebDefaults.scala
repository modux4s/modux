package modux.plugin.web

import sbt.*
import sbt.Keys.*

case object ModuxWebDefaults {
  private val pac4jVersion: String = buildinfo.BuildInfo.pac4jVersion

  private val pac4jCode: ModuleID = "org.pac4j" % "pac4j-core" % pac4jVersion
  private val pac4jOAuth: ModuleID = "org.pac4j" % "pac4j-oauth" % pac4jVersion
  private val pac4jHttp: ModuleID = "org.pac4j" % "pac4j-http" % pac4jVersion
  private val pac4jJwt: ModuleID = "org.pac4j" % "pac4j-jwt" % pac4jVersion

  val webSecurityDeps: Seq[ModuleID] = Seq(pac4jCode, pac4jOAuth, pac4jHttp, pac4jJwt)

  val defaultSettings: Seq[Setting[_]] = Seq[Setting[_]](
    libraryDependencies ++= Seq(pac4jCode, pac4jOAuth, pac4jHttp, pac4jJwt)
  )
}

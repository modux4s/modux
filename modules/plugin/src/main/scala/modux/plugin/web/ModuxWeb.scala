package modux.plugin.web

import com.typesafe.sbt.web.SbtWeb
import play.twirl.sbt.SbtTwirl
import sbt.plugins.JvmPlugin
import sbt.{Def, _}

case object ModuxWeb extends AutoPlugin {
  override def requires: Plugins = JvmPlugin && SbtTwirl && SbtWeb

  override def projectSettings: Seq[Def.Setting[_]] = ModuxWebDefaults.defaultSettings
}

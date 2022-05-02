package modux.plugin

import modux.plugin.core.CommonSettings
import modux.plugin.service.ModuxService
import sbt.plugins.JvmPlugin
import sbt.{Def, *}

case object ModuxPlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin && ModuxService

  override def projectSettings: Seq[Def.Setting[?]] = CommonSettings.projectLayout ++ CommonSettings.packageSettings
}

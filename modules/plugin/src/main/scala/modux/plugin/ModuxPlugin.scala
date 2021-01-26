package modux.plugin

import modux.plugin.core.CommonSettings
import modux.plugin.service.ModuxService
import modux.plugin.web.ModuxWeb
import sbt.plugins.JvmPlugin
import sbt.{Def, _}

case object ModuxPlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin && ModuxService

  override def projectSettings: Seq[Def.Setting[_]] = CommonSettings.projectLayout ++ CommonSettings.packageSettings
}

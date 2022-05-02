package modux.plug

import sbt.plugins.JvmPlugin
import sbt.{AutoPlugin, Def, Plugins}

object HookPlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin

  object autoImport extends CommonSettings {
  }

  import autoImport.*

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    startHook := Nil,
    stopHook := Nil
  )
}

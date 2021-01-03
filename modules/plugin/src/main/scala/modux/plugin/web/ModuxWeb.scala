package modux.plugin.web

import play.twirl.sbt.Import.TwirlKeys
import play.twirl.sbt.SbtTwirl
import sbt.Keys.{libraryDependencies, sourceDirectories, sourceDirectory}
import sbt.plugins.JvmPlugin
import sbt.{Def, _}

case object ModuxWeb extends AutoPlugin {
  override def requires: Plugins = JvmPlugin && SbtTwirl

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    sourceDirectories in(Compile, TwirlKeys.compileTemplates) := Seq(sourceDirectory.value)
  ) ++ ModuxWebDefaults.defaultSettings
}

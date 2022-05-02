package modux.plug

import sbt.*

trait CommonSettings {
  val startHook = settingKey[Seq[Def.Initialize[Task[Unit]]]]("start hooks")
  val stopHook = settingKey[Seq[Def.Initialize[Task[Unit]]]]("start hooks")
}

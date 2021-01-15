package modux.plugin.core

import modux.shared.BuildContext

object Types {
  type Server = {
    def reload(buildContext: BuildContext): Unit
    def init(buildContext: BuildContext): Unit
    def exporter(buildContext: BuildContext): String
    def stop(): Unit
  }
}

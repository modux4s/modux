package modux.server.model

import modux.shared.BuildContext

object Types {
  type ExporterResolver = {
    def processor(buildContext: BuildContext): String
  }
}

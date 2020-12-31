package example.server

import modux.core.api.{ModuleX, Service}

class Module extends ModuleX {
  override def providers: Seq[Service] = Seq(
    wire[SimpleTest]
  )
}

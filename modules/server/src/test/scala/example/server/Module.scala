package example.server

import modux.core.api.{ModuleX, Service}
import modux.model.context.Context

case class Module(context: Context) extends ModuleX {
  override def providers: Seq[Service] = Seq(
    SimpleTest(context)
  )
}

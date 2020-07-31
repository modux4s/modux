package mserver.example

import modux.core.api.{ModuleX, Service}
import modux.model.context.Context
import com.softwaremill.macwire.wire

case class Module(context: Context) extends ModuleX {
  override def providers: Seq[Service] = Seq(
    wire[UserServiceImpl]
  )
}

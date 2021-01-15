package modux.core.api

import modux.macros.di.MacwireSupport
import modux.model.context.ContextSupport

trait ModuleX extends ContextSupport with MacwireSupport{
  def onStart(): Unit = {}
  def onStop(): Unit = {}
  def providers: Seq[Service]

}

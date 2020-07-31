package modux.core.api

import modux.model.context.ContextSupport

trait ModuleX extends ContextSupport {
  def onStart(): Unit = {}
  def onStop(): Unit = {}
  def providers: Seq[Service]

}

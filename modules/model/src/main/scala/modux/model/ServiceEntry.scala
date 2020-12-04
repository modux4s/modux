package modux.model

trait ServiceEntry {
  def onStart(): Unit

  def onStop(): Unit
}

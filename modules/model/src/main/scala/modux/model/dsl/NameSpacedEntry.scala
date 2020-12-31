package modux.model.dsl

final case class NameSpacedEntry(namespace:String, entries: Seq[RestEntry]) extends MultiRestEntry {
  override def onStart(): Unit = {}

  override def onStop(): Unit = {}
}

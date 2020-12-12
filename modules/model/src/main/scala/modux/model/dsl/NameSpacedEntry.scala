package modux.model.dsl

import modux.model.ServiceEntry

final case class NameSpacedEntry(ns:String, restEntry: Seq[RestEntry]) extends ServiceEntry {
  override def onStart(): Unit = {}

  override def onStop(): Unit = {}
}

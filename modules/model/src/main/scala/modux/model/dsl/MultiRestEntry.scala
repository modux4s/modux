package modux.model.dsl

import modux.model.ServiceEntry

trait MultiRestEntry extends ServiceEntry{
  def entries: Seq[RestEntry]
}

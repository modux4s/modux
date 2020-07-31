package modux.model

sealed trait RestEntryMeta {
}

final case class Single(v:String) extends RestEntryMeta
final case class Multi(v:Seq[String]) extends RestEntryMeta

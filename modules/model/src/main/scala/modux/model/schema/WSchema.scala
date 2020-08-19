package modux.model.schema

sealed trait WSchema {
  def name:String
}

final case class WRefSchema(name:String, ref:String) extends WSchema
final case class WPrimitiveSchema(name:String, kind:String) extends WSchema

package modux.model.schema

case class MParameter(name: String, in: String, required: Boolean, schema: Option[MSchema], pattern:Option[String] = None)

package modux.model.schema

case class MParameter(name: String, in: String, required: Boolean, schema: Option[MSchema])

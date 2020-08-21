package modux.model.schema

sealed trait MSchema

final case class MRefSchema(ref: String, isNullable:Boolean) extends MSchema

final case class MPrimitiveSchema(
                                   kind: String,
                                   isPrimitive: Boolean,
                                   isNullable: Boolean,
                                   format: Option[String],
                                   example: Option[String],
                                   properties: Map[String, MSchema]
                                 ) extends MSchema

final case class MArraySchema(item: MSchema) extends MSchema

final case class MComposed(items: Seq[MSchema]) extends MSchema
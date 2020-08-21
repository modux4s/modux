package modux.model.exporter

import modux.model.schema.MSchema

case class SchemaDescriptor(reference: MSchema, references: Map[String, MSchema])

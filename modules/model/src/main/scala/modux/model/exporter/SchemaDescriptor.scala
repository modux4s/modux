package modux.model.exporter

import io.swagger.v3.oas.models.media.Schema

case class SchemaDescriptor(reference: Schema[_], references: Map[String, Schema[_]])

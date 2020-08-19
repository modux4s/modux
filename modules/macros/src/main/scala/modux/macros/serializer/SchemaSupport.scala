package modux.macros.serializer

import modux.macros.service.ServiceSupportMacro
import modux.model.exporter.{EvidenceDescriptor, SchemaDescriptor}

trait SchemaSupport {
  protected def by[A]: Option[SchemaDescriptor] = macro ServiceSupportMacro.extractSchemaMacro[A]
  protected def instanceOf[A]: Option[SchemaDescriptor] = macro ServiceSupportMacro.extractSchemaMacro[A]
}

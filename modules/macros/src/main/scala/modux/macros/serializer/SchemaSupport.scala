package modux.macros.serializer

import modux.macros.service.ServiceSupportMacro
import modux.model.exporter.SchemaDescriptor

trait SchemaSupport {
  protected def item[A]: Option[SchemaDescriptor] = macro ServiceSupportMacro.extractSchemaMacro[A]
}

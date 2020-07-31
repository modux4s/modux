package modux.core.api

import modux.common.FutureImplicits
import modux.core.support.RegisterDSL
import modux.macros.serializer.SchemaSupport
import modux.macros.service.MethodDSL
import modux.model.context.ContextSupport
import modux.model.dsl.ResponseDSL

trait Service extends RegisterDSL with MethodDSL with ResponseDSL with FutureImplicits with ContextSupport with SchemaSupport {
}

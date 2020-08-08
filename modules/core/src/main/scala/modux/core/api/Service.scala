package modux.core.api

import modux.common.FutureImplicits
import modux.core.support.RegisterDSL
import modux.macros.serializer.SchemaSupport
import modux.macros.service.{MethodDSL, ResponseDSL}
import modux.model.context.ContextSupport

trait Service extends RegisterDSL with MethodDSL with ResponseDSL with FutureImplicits with ContextSupport with SchemaSupport {
}

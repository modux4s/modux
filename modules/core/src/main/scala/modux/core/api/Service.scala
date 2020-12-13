package modux.core.api

import modux.common.FutureImplicits
import modux.core.support.{RegisterDSL, ResponseExtensions}
import modux.macros.serializer.SchemaSupport
import modux.macros.service.MethodDSL
import modux.model.context.ContextSupport
import modux.model.service.CallDirectives

trait Service extends CallDirectives with RegisterDSL with MethodDSL with ResponseExtensions with FutureImplicits with ContextSupport with SchemaSupport
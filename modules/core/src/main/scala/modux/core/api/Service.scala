package modux.core.api

import modux.common.FutureImplicits
import modux.core.support.RegisterDSL
import modux.macros.di.MacwireSupport
import modux.macros.serializer.SchemaSupport
import modux.macros.service.MethodDSL
import modux.model.context.ContextSupport
import modux.model.directives.CallDirectives

trait Service extends CallDirectives
  with RegisterDSL
  with MethodDSL
  with FutureImplicits
  with ContextSupport
  with SchemaSupport
  with MacwireSupport
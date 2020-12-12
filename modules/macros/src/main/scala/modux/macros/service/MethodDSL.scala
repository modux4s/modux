package modux.macros.service

import modux.model.dsl.RestEntry
import modux.model.rest.RestService
import modux.model.service.Call

//noinspection ScalaUnusedSymbol
trait MethodDSL {
  protected final def statics(url: String, dir: String): RestEntry = macro ServiceSupportMacro.staticServe
  protected final def get[IN, OUT](url: String, f: Any): RestEntry = macro ServiceMacroInvoke.get
  protected final def post[IN, OUT](url: String, f: Any): RestEntry = macro ServiceMacroInvoke.post
  protected final def put[IN, OUT](url: String, f: Any): RestEntry = macro ServiceMacroInvoke.put
  protected final def delete[IN, OUT](url: String, f: Any): RestEntry = macro ServiceMacroInvoke.delete
  protected final def patch[IN, OUT](url: String, f: Any): RestEntry = macro ServiceMacroInvoke.patch
  protected final def named[IN, OUT](url: String, f: Any): RestEntry = macro ServiceMacroInvoke.name

}

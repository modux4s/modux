package modux.macros.service

import modux.model.RestService
import modux.model.service.Call

//noinspection ScalaUnusedSymbol
trait MethodDSL {
  protected final def statics(url: String, dir: String): RestService = macro ServiceSupportMacro.staticServe
  protected final def get[IN, OUT](url: String, f: Any): RestService = macro ServiceMacroInvoke.get
  protected final def post[IN, OUT](url: String, f: Any): RestService = macro ServiceMacroInvoke.post
  protected final def put[IN, OUT](url: String, f: Any): RestService = macro ServiceMacroInvoke.put
  protected final def delete[IN, OUT](url: String, f: Any): RestService = macro ServiceMacroInvoke.delete
  protected final def patch[IN, OUT](url: String, f: Any): RestService = macro ServiceMacroInvoke.patch
  protected final def named[IN, OUT](url: String, f: Any): RestService = macro ServiceMacroInvoke.name
}

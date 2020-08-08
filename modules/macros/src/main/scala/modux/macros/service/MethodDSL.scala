package modux.macros.service

import modux.model.RestService
import modux.model.service.Call

//noinspection ScalaUnusedSymbol
trait MethodDSL {

  protected final def statics(url: String, dir: String): RestService = macro ServiceSupportMacro.staticServe

  protected final def get[IN, OUT](url: String, f: () => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.get
  protected final def get[A1, IN, OUT](url: String, f: A1 => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.get
  protected final def get[A1, A2, IN, OUT](url: String, f: (A1, A2) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.get
  protected final def get[IN, OUT, A1, A2, A3](url: String, f: (A1, A2, A3) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.get
  protected final def get[IN, OUT, A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.get
  protected final def get[IN, OUT, A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.get

  protected final def post[IN, OUT](url: String, f: () => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.post
  protected final def post[IN, OUT, A1](url: String, f: A1 => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.post
  protected final def post[IN, OUT, A1, A2](url: String, f: (A1, A2) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.post
  protected final def post[IN, OUT, A1, A2, A3](url: String, f: (A1, A2, A3) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.post
  protected final def post[IN, OUT, A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.post
  protected final def post[IN, OUT, A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.post

  protected final def put[IN, OUT](url: String, f: () => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.put
  protected final def put[IN, OUT, A1](url: String, f: A1 => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.put
  protected final def put[IN, OUT, A1, A2](url: String, f: (A1, A2) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.put
  protected final def put[IN, OUT, A1, A2, A3](url: String, f: (A1, A2, A3) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.put
  protected final def put[IN, OUT, A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.put
  protected final def put[IN, OUT, A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.put

  protected final def delete[IN, OUT](url: String, f: () => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.delete
  protected final def delete[IN, OUT, A1](url: String, f: A1 => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.delete
  protected final def delete[IN, OUT, A1, A2](url: String, f: (A1, A2) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.delete
  protected final def delete[IN, OUT, A1, A2, A3](url: String, f: (A1, A2, A3) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.delete
  protected final def delete[IN, OUT, A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.delete
  protected final def delete[IN, OUT, A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.delete

  protected final def patch[IN, OUT](url: String, f: () => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.patch
  protected final def patch[IN, OUT, A1](url: String, f: A1 => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.patch
  protected final def patch[IN, OUT, A1, A2](url: String, f: (A1, A2) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.patch
  protected final def patch[IN, OUT, A1, A2, A3](url: String, f: (A1, A2, A3) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.patch
  protected final def patch[IN, OUT, A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.patch
  protected final def patch[IN, OUT, A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.patch

  protected final def named[IN, OUT](url: String, f: () => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.name
  protected final def named[IN, OUT, A1](url: String, f: A1 => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.name
  protected final def named[IN, OUT, A1, A2](url: String, f: (A1, A2) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.name
  protected final def named[IN, OUT, A1, A2, A3](url: String, f: (A1, A2, A3) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.name
  protected final def named[IN, OUT, A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.name
  protected final def named[IN, OUT, A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[IN, OUT]): RestService = macro ServiceMacroInvoke.name
}

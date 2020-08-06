package modux.macros.service

import modux.model.RestService
import modux.model.service.Call

//noinspection ScalaUnusedSymbol
trait MethodDSL {

  protected def statics(url: String, dir: String): RestService = macro ServiceSupportMacro.staticServe

  protected def get(url: String, f: () => Call[_, _]): RestService = macro ServiceMacroInvoke.get
  protected def get[A1](url: String, f: A1 => Call[_, _]): RestService = macro ServiceMacroInvoke.get
  protected def get[A1, A2](url: String, f: (A1, A2) => Call[_, _]): RestService = macro ServiceMacroInvoke.get
  protected def get[A1, A2, A3](url: String, f: (A1, A2, A3) => Call[_, _]): RestService = macro ServiceMacroInvoke.get
  protected def get[A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[_, _]): RestService = macro ServiceMacroInvoke.get
  protected def get[A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[_, _]): RestService = macro ServiceMacroInvoke.get

  protected def post(url: String, f: () => Call[_, _]): RestService = macro ServiceMacroInvoke.post
  protected def post[A1](url: String, f: A1 => Call[_, _]): RestService = macro ServiceMacroInvoke.post
  protected def post[A1, A2](url: String, f: (A1, A2) => Call[_, _]): RestService = macro ServiceMacroInvoke.post
  protected def post[A1, A2, A3](url: String, f: (A1, A2, A3) => Call[_, _]): RestService = macro ServiceMacroInvoke.post
  protected def post[A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[_, _]): RestService = macro ServiceMacroInvoke.post
  protected def post[A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[_, _]): RestService = macro ServiceMacroInvoke.post

  protected def put(url: String, f: () => Call[_, _]): RestService = macro ServiceMacroInvoke.put
  protected def put[A1](url: String, f: A1 => Call[_, _]): RestService = macro ServiceMacroInvoke.put
  protected def put[A1, A2](url: String, f: (A1, A2) => Call[_, _]): RestService = macro ServiceMacroInvoke.put
  protected def put[A1, A2, A3](url: String, f: (A1, A2, A3) => Call[_, _]): RestService = macro ServiceMacroInvoke.put
  protected def put[A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[_, _]): RestService = macro ServiceMacroInvoke.put
  protected def put[A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[_, _]): RestService = macro ServiceMacroInvoke.put

  protected def delete(url: String, f: () => Call[_, _]): RestService = macro ServiceMacroInvoke.delete
  protected def delete[A1](url: String, f: A1 => Call[_, _]): RestService = macro ServiceMacroInvoke.delete
  protected def delete[A1, A2](url: String, f: (A1, A2) => Call[_, _]): RestService = macro ServiceMacroInvoke.delete
  protected def delete[A1, A2, A3](url: String, f: (A1, A2, A3) => Call[_, _]): RestService = macro ServiceMacroInvoke.delete
  protected def delete[A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[_, _]): RestService = macro ServiceMacroInvoke.delete
  protected def delete[A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[_, _]): RestService = macro ServiceMacroInvoke.delete

  protected def patch(url: String, f: () => Call[_, _]): RestService = macro ServiceMacroInvoke.patch
  protected def patch[A1](url: String, f: A1 => Call[_, _]): RestService = macro ServiceMacroInvoke.patch
  protected def patch[A1, A2](url: String, f: (A1, A2) => Call[_, _]): RestService = macro ServiceMacroInvoke.patch
  protected def patch[A1, A2, A3](url: String, f: (A1, A2, A3) => Call[_, _]): RestService = macro ServiceMacroInvoke.patch
  protected def patch[A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[_, _]): RestService = macro ServiceMacroInvoke.patch
  protected def patch[A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[_, _]): RestService = macro ServiceMacroInvoke.patch

  protected def named(url: String, f: () => Call[_, _]): RestService = macro ServiceMacroInvoke.name
  protected def named[A1](url: String, f: A1 => Call[_, _]): RestService = macro ServiceMacroInvoke.name
  protected def named[A1, A2](url: String, f: (A1, A2) => Call[_, _]): RestService = macro ServiceMacroInvoke.name
  protected def named[A1, A2, A3](url: String, f: (A1, A2, A3) => Call[_, _]): RestService = macro ServiceMacroInvoke.name
  protected def named[A1, A2, A3, A4](url: String, f: (A1, A2, A3, A4) => Call[_, _]): RestService = macro ServiceMacroInvoke.name
  protected def named[A1, A2, A3, A4, A5](url: String, f: (A1, A2, A3, A4, A5) => Call[_, _]): RestService = macro ServiceMacroInvoke.name
}

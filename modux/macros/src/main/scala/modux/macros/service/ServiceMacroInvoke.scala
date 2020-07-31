package modux.macros.service

import modux.model.RestService

import scala.reflect.macros.blackbox

object ServiceMacroInvoke {

  def name(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestService] = {
    ServiceSupportMacro.callRestBuilder(c)(checkName = true, url, f)
  }

  def patch(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestService] = {
    ServiceSupportMacro.callRestBuilder(c)(checkName = false, url, f)
  }

  def get(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestService] = {
    ServiceSupportMacro.restServiceBuilder(c)("get", url, f)
  }

  def post(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestService] = {
    ServiceSupportMacro.restServiceBuilder(c)("post", url, f)
  }

  def put(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestService] = {
    ServiceSupportMacro.restServiceBuilder(c)("put", url, f)
  }

  def delete(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestService] = {
    ServiceSupportMacro.restServiceBuilder(c)("delete", url, f)
  }
}

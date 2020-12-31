package modux.macros.service

import modux.model.dsl.RestEntry

import scala.reflect.macros.blackbox

object ServiceMacroInvoke {

  def named(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestEntry] = {
    ServiceSupportMacro.callRestBuilder(c)(checkName = true, url, f)
  }

  def patch(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestEntry] = {
    ServiceSupportMacro.callRestBuilder(c)(checkName = false, url, f)
  }

  def get(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestEntry] = {
    ServiceSupportMacro.restServiceBuilder(c)(Some("get"), url, f)
  }

  def post(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestEntry] = {
    ServiceSupportMacro.restServiceBuilder(c)(Some("post"), url, f)
  }

  def put(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestEntry] = {
    ServiceSupportMacro.restServiceBuilder(c)(Some("put"), url, f)
  }

  def delete(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestEntry] = {
    ServiceSupportMacro.restServiceBuilder(c)(Some("delete"), url, f)
  }

  def head(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestEntry] = {
    ServiceSupportMacro.restServiceBuilder(c)(Some("head"), url, f)
  }

  def call(c: blackbox.Context)(url: c.Expr[String], f: c.Tree): c.Expr[RestEntry] = {
    ServiceSupportMacro.restServiceBuilder(c)(None, url, f)
  }
}

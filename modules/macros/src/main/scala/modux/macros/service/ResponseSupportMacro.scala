package modux.macros.service

import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model.StatusCode

import scala.reflect.macros.blackbox

object ResponseSupportMacro {

  def NotFoundEmpty(c: blackbox.Context): c.Expr[Nothing] = genericCode(c)(404)
  def NotFound[A: c.WeakTypeTag](c: blackbox.Context)(d: c.Expr[A]): c.Expr[Nothing] = genericData(c)(404, d)
  def BadRequestEmpty(c: blackbox.Context): c.Expr[Nothing] = genericCode(c)(400)
  def BadRequest[A: c.WeakTypeTag](c: blackbox.Context)(d: c.Expr[A]): c.Expr[Nothing] = genericData(c)(400, d)
  def UnauthorizedEmpty(c: blackbox.Context): c.Expr[Nothing] = genericCode(c)(401)
  def Unauthorized[A: c.WeakTypeTag](c: blackbox.Context)(d: c.Expr[A]): c.Expr[Nothing] = genericData(c)(401, d)
  def InternalErrorEmpty(c: blackbox.Context): c.Expr[Nothing] = genericCode(c)(500)
  def InternalError[A: c.WeakTypeTag](c: blackbox.Context)(d: c.Expr[A]): c.Expr[Nothing] = genericData(c)(500, d)


  def okDatum[A: c.WeakTypeTag](c: blackbox.Context)(d: c.Expr[A]): c.Expr[A] = d

  def genericCode(c: blackbox.Context)(code: Int): c.Expr[Nothing] = {
    c.Expr[Nothing](c.parse(s"throw modux.model.dsl.ResponseAsFalseFail(modux.macros.service.ResponseSupportMacro.asStatusCode($code))"))
  }

  def genericData[A: c.WeakTypeTag](c: blackbox.Context)(code: Int, d: c.Expr[A]): c.Expr[Nothing] = {
    c.Expr[Nothing](c.parse(s"throw modux.model.dsl.ResponseAsFalseFail(modux.macros.service.ResponseSupportMacro.asEntity($code, ${d.tree}))"))
  }

  def responseWithEmpty(c: blackbox.Context)(code: c.Expr[Int]): c.Expr[Nothing] = {
    c.Expr[Nothing](c.parse(s"throw modux.model.dsl.ResponseAsFalseFail(modux.macros.service.ResponseSupportMacro.asStatusCode(${code.tree}))"))
  }

  def responseWith[A: c.WeakTypeTag](c: blackbox.Context)(code: c.Expr[Int], d: c.Expr[A]): c.Expr[Nothing] = {
    c.Expr[Nothing](c.parse(s"throw modux.model.dsl.ResponseAsFalseFail(modux.macros.service.ResponseSupportMacro.asEntity(${code.tree}, ${d.tree}))"))
  }

  def asStatusCode(code: Int)(implicit m: ToResponseMarshaller[StatusCode]): ToResponseMarshallable = new ToResponseMarshallable {
    override type T = StatusCode

    override def value: StatusCode = StatusCode.int2StatusCode(code)

    override implicit def marshaller: ToResponseMarshaller[StatusCode] = m
  }

  def asEntity[A](code: Int, data: A)(implicit m: ToResponseMarshaller[(Int, A)]): ToResponseMarshallable = new ToResponseMarshallable {
    override type T = (Int, A)

    override def value: (Int, A) = (code, data)

    override implicit def marshaller: ToResponseMarshaller[(Int, A)] = m
  }
}

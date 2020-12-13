package modux.macros.service

import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.{HttpHeader, HttpResponse, StatusCode}
import com.typesafe.scalalogging.LazyLogging

import scala.reflect.macros.blackbox

object ResponseSupportMacro extends LazyLogging {

  private def asInt(c: blackbox.Context)(code: Int): c.Expr[Int] = c.Expr[Int](c.parse(code.toString))

  private def emptySeq(c: blackbox.Context): c.Expr[Seq[(String, String)]] = c.Expr[Seq[(String, String)]](c.parse("Nil"))

  def NotFoundEmpty(c: blackbox.Context): c.Expr[Nothing] = genericCode(c)(asInt(c)(404), emptySeq(c))

  def NotFound[A: c.WeakTypeTag](c: blackbox.Context)(d: c.Expr[A]): c.Expr[Nothing] = genericData(c)(404, d)

  def Redirect(c: blackbox.Context)(url: c.Expr[String], code: c.Expr[Int]): c.Expr[Nothing] = genericCode(c)(code, c.Expr[Seq[(String, String)]](c.parse(s"""Seq( ("Location", ${url.tree}) )""")))

  def BadRequestEmpty(c: blackbox.Context): c.Expr[Nothing] = genericCode(c)(asInt(c)(400), emptySeq(c))

  def BadRequest[A: c.WeakTypeTag](c: blackbox.Context)(d: c.Expr[A]): c.Expr[Nothing] = genericData(c)(400, d)

  def UnauthorizedEmpty(c: blackbox.Context): c.Expr[Nothing] = genericCode(c)(asInt(c)(401), emptySeq(c))

  def Unauthorized[A: c.WeakTypeTag](c: blackbox.Context)(d: c.Expr[A]): c.Expr[Nothing] = genericData(c)(401, d)

  def InternalErrorEmpty(c: blackbox.Context): c.Expr[Nothing] = genericCode(c)(asInt(c)(500), emptySeq(c))

  def InternalError[A: c.WeakTypeTag](c: blackbox.Context)(d: c.Expr[A]): c.Expr[Nothing] = genericData(c)(500, d)

  def genericCode(c: blackbox.Context)(code: c.Expr[Int], headers: c.Expr[Seq[(String, String)]]): c.Expr[Nothing] = {
    c.Expr[Nothing](c.parse(s"throw modux.model.dsl.ResponseAsFalseFail(modux.macros.service.ResponseSupportMacro.asStatusCode(${code.tree}, ${headers.tree}))"))
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

  def asStatusCode(code: Int, headers: Seq[(String, String)])(implicit m: ToResponseMarshaller[HttpResponse]): ToResponseMarshallable = new ToResponseMarshallable {
    override type T = HttpResponse

    override def value: HttpResponse = {
      HttpResponse(
        status = StatusCode.int2StatusCode(code),
        headers = headers.flatMap { case (str, str1) =>
          HttpHeader.parse(str, str1) match {
            case ParsingResult.Ok(header, _) => Some(header)
            case ParsingResult.Error(error) =>
              logger.error(error.summary)
              None
          }
        }.toList
      )
    }

    override implicit def marshaller: ToResponseMarshaller[HttpResponse] = m
  }

  def asEntity[A](code: Int, data: A)(implicit m: ToResponseMarshaller[(Int, A)]): ToResponseMarshallable = new ToResponseMarshallable {
    override type T = (Int, A)

    override def value: (Int, A) = (code, data)

    override implicit def marshaller: ToResponseMarshaller[(Int, A)] = m
  }
}

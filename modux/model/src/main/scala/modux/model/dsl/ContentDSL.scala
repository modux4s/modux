package modux.model.dsl

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import modux.model.content.{Content, WithContent, WithoutContent}


object ContentDSL extends ContentDSL

trait ContentDSL {
  private implicit def asOption[T](v: T): Option[T] = Option(v)

  def Ok[T](body: T)(implicit m: ToEntityMarshaller[T]): Content = WithContent(200, body)
  def Ok: Content = WithoutContent(200)

  def NotFound[T](body: T)(implicit m: ToEntityMarshaller[T]): Content = WithContent(404, body)
  def NotFound(msg: String): Content = WithoutContent(404, msg)
  def NotFound: Content = WithoutContent(404)
  def Unauthorized: Content = WithoutContent(401)
  def Unauthorized(msg:String): Content = WithoutContent(401, msg)
  def InternalError(msg: String): Content = WithoutContent(500, msg)
}


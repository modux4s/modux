package modux.model.instruction.impl

import akka.http.scaladsl.model.headers.{HttpCookie, `Set-Cookie`}
import akka.http.scaladsl.model.{AttributeKey, ContentType, HttpHeader, HttpResponse}
import modux.model.instruction.Instruction

package object response {

  sealed trait ResponseCommand

  case class ContentAs(contentType: ContentType) extends ResponseCommand

  case class AddHeaders(httpHeader: List[HttpHeader]) extends ResponseCommand

  case class AddCookies(cookies: List[HttpCookie]) extends ResponseCommand

  case class AddAttributes(attributes: Map[String, AnyRef]) extends ResponseCommand

  case class SetStatus(statusCode: Int) extends ResponseCommand

  object ResponseInstruction extends Instruction[ResponseCommand, HttpResponse] {
    override def apply(effect: HttpResponse, command: ResponseCommand): HttpResponse = command match {
      case ContentAs(contentType) => effect.mapEntity(x => x.withContentType(contentType))
      case AddHeaders(httpHeaders) => httpHeaders.foldLeft(effect) { case (item, x) => item.addHeader(x) }
      case AddCookies(cookies) => cookies.foldLeft(effect) { case (item, x) => item.addHeader(`Set-Cookie`(x)) }
      case SetStatus(statusCode) =>  effect.withStatus(statusCode)
      case AddAttributes(attributes) => attributes.foldLeft(effect) { case (item, x) => item.addAttribute(AttributeKey(x._1), x._2) }
    }
  }

}

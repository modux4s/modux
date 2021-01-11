package modux.model.header

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.{ContentType, ContentTypes, HttpHeader, MediaTypes}
import com.typesafe.scalalogging.LazyLogging
import modux.model.instruction.impl.response._

import scala.collection.mutable.ArrayBuffer

final case class ResponseHeader() extends LazyLogging {

  private val instructions: ArrayBuffer[ResponseCommand] = ArrayBuffer.empty

  private def addInstruction(instruction: ResponseCommand): ResponseHeader = {
    instructions.append(instruction)
    this
  }

  private[modux] def getInstructions: ArrayBuffer[ResponseCommand] = instructions

  def withStatus(code: Int): ResponseHeader = addInstruction(SetStatus(code))

  def withHeader(n: String, v: String): ResponseHeader = {
    parseHeader(n, v).fold(this)(x => addInstruction(AddHeaders(List(x))))
  }

  def withHeaders(v: (String, String)*): ResponseHeader = {
    val mod: List[HttpHeader] = v.foldLeft(List.empty[HttpHeader]) { case (acc, item) =>
      parseHeader(item._1, item._2).fold(acc)(x => acc :+ x)
    }
    addInstruction(AddHeaders(mod))
  }

  def withAttributes(items: Map[String, AnyRef]): ResponseHeader = addInstruction(AddAttributes(items))

  def getAttribute(key: String): Option[AnyRef] = {
    instructions.collect{ case AddAttributes(attributes) => attributes.get(key)}.flatten.headOption
  }

  def withHeaders(items: List[HttpHeader]): ResponseHeader = addInstruction(AddHeaders(items))

  def withHeader(v: HttpHeader): ResponseHeader = addInstruction(AddHeaders(List(v)))

  def withCookie(c: HttpCookie): ResponseHeader = addInstruction(AddCookies(List(c)))

  def withCookie(n: String, v: String): ResponseHeader = addInstruction(AddCookies(List(HttpCookie(n, v))))

  def withCookies(v: List[HttpCookie]): ResponseHeader = addInstruction(AddCookies(v))

  def withCookies(v: Map[String, String]): ResponseHeader = addInstruction(AddCookies(v.map { case (k, v) => HttpCookie(k, v) }.toList))

  def asJson: ResponseHeader = addInstruction(ContentAs(ContentTypes.`application/json`))

  def asXml: ResponseHeader = addInstruction(ContentAs(MediaTypes.`application/xml`.toContentTypeWithMissingCharset))

  def asHtml: ResponseHeader = addInstruction(ContentAs(ContentTypes.`text/html(UTF-8)`))

  def asTextPlain: ResponseHeader = addInstruction(ContentAs(ContentTypes.`text/plain(UTF-8)`))

  def withContentType(ct: ContentType): ResponseHeader = {
    addInstruction(ContentAs(ct))
  }

  def withContentType(ct: String): ResponseHeader = {
    ContentType.parse(ct) match {
      case Left(value) =>
        value.foreach(x => logger.error(x.detail))
        this
      case Right(value) => addInstruction(ContentAs(value))
    }

  }

  private def parseHeader(n: String, v: String): Option[HttpHeader] = {
    HttpHeader.parse(n, v) match {

      case ParsingResult.Ok(header, errors) =>
        errors.foreach(x => logger.warn(x.summary, x.detail))
        Option(header)
      case ParsingResult.Error(error) =>
        logger.warn(error.summary, error.detail)
        None
    }
  }
}



package modux.model.header

import akka.http.javadsl.model.Uri
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.headers.HttpCookie
import com.typesafe.scalalogging.LazyLogging

final case class ResponseHeader(
                                 status: Int,
                                 uri: Uri  = Uri.EMPTY,
                                 contentAs: ContentAs = Default,
                                 headers: List[HttpHeader] = Nil,
                                 cookies: Map[String, HttpCookie] = Map.empty
                               ) extends LazyLogging {

  def withStatus(v: Int): ResponseHeader = copy(status = v)

  def withHeader(n: String, v: String): ResponseHeader = {
    parseHeader(n, v).fold(this)(x => copy(headers = headers :+ x))
  }

  def withHeaders(v: (String, String)*): ResponseHeader = {
    val mod: Seq[HttpHeader] = v.foldLeft(Seq.empty[HttpHeader]) { case (acc, item) =>
      parseHeader(item._1, item._2).fold(acc)(x => acc :+ x)
    }
    copy(headers = headers ++ mod)
  }

  def withHeaders(items: List[HttpHeader]): ResponseHeader = copy(headers = headers ++ items)

  def withHeader(v: HttpHeader): ResponseHeader = copy(headers = headers :+ v)

  def withCookie(n: String, v: String): ResponseHeader = copy(cookies = cookies + (n -> HttpCookie(n, v)))

  def withCookies(v: HttpCookie*): ResponseHeader = copy(cookies = cookies ++ v.map { x => x.name() -> x }.toMap)

  def withCookies(v: Map[String, String]): ResponseHeader = copy(cookies = cookies ++ v.map { case (k, v) => k -> HttpCookie(k, v) })

  def asJson: ResponseHeader = copy(contentAs = Json)

  def asXml: ResponseHeader = copy(contentAs = Xml)

  def asHtml: ResponseHeader = copy(contentAs = Html)

  def asTextPlain: ResponseHeader = copy(contentAs = TextPlain)

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

object ResponseHeader {
  final val Empty: ResponseHeader = ResponseHeader(200)
}
package modux.model.header

import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.headers.HttpCookie

sealed trait ContentAs

case object Default extends ContentAs

case object Json extends ContentAs

case object Xml extends ContentAs

case object TextPlain extends ContentAs

case object Html extends ContentAs

case class Custom(contentType: ContentType) extends ContentAs

trait ResponseHeader {
  def status: Int
  def uri: String
  def headers: Map[String, String]
  def cookies: Map[String, HttpCookie]
  def contentAs: ContentAs
  //  def deleteCookies(): Set[String]

  def withStatus(v: Int): ResponseHeader

  def withHeader(n: String, v: String): ResponseHeader
  def withHeaders(v: (String, String)*): ResponseHeader
  def withHeaders(v: Map[String, String]): ResponseHeader

  def withCookie(n: String, v: String): ResponseHeader
  def withCookies(cookies: HttpCookie*): ResponseHeader
  def withCookies(v: Map[String, String]): ResponseHeader

  //  def removeCookies(v:String*): ResponseHeader

  def asJson: ResponseHeader
  def asXml: ResponseHeader
  def asHtml: ResponseHeader
  def asTextPlain: ResponseHeader
}

final case class ResponseHeaderImpl(
                                     status: Int,
                                     uri: String,
                                     contentAs: ContentAs = Default,
                                     headers: Map[String, String] = Map.empty,
                                     cookies: Map[String, HttpCookie] = Map.empty,
                                     //                                     deleteCookies: Set[String] = Set.empty
                                   ) extends ResponseHeader {

  override def withStatus(v: Int): ResponseHeader = copy(status = v)

  override def withHeader(n: String, v: String): ResponseHeader = copy(headers = headers + (n -> v))

  override def withHeaders(v: (String, String)*): ResponseHeader = copy(headers = headers ++ v.toMap)

  override def withHeaders(v: Map[String, String]): ResponseHeader = copy(headers = headers ++ v)

  override def withCookie(n: String, v: String): ResponseHeader = copy(cookies = cookies + (n -> HttpCookie(n, v)))

  override def withCookies(v: HttpCookie*): ResponseHeader = copy(cookies = cookies ++ v.map { x => x.name() -> x }.toMap)

  override def withCookies(v: Map[String, String]): ResponseHeader = copy(cookies = cookies ++ v.map { case (k, v) => k -> HttpCookie(k, v) })

  override def asJson: ResponseHeader = copy(contentAs = Json)

  override def asXml: ResponseHeader = copy(contentAs = Xml)

  override def asHtml: ResponseHeader = copy(contentAs = Html)

  override def asTextPlain: ResponseHeader = copy(contentAs = TextPlain)
}

object ResponseHeader {
  final val Default: ResponseHeader = ResponseHeaderImpl(200, "/")
}
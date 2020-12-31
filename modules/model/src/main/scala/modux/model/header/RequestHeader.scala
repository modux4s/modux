package modux.model.header

import akka.http.javadsl.model.{Query, Uri}
import akka.http.scaladsl.model.{HttpMethod, HttpMethods}

import scala.jdk.CollectionConverters.mapAsScalaMapConverter

final case class RequestHeader private(
                                        method: HttpMethod,
                                        uri: Uri,
                                        headers: Map[String, String],
                                        cookies: Map[String, String],
                                        removeCookies: Set[String]
                                      ) {
  lazy val query: Query = uri.query()
  lazy val queryParams: Map[String, String] = query.toMap.asScala.toMap

  def hasHeader(n: String): Boolean = headers.contains(n)

  def removeCookie(n: String): RequestHeader = copy(removeCookies = removeCookies - n)

  def withHeader(n: String, v: String): RequestHeader = copy(headers = headers + (n -> v))

  def withHeaders(v: (String, String)*): RequestHeader = copy(headers = headers ++ v.toMap)

  def withHeaders(v: Map[String, String]): RequestHeader = copy(headers = headers ++ v)

  def withCookie(n: String, v: String): RequestHeader = copy(cookies = cookies + (n -> v))

  def withCookies(v: (String, String)*): RequestHeader = copy(cookies = cookies ++ v.toMap)

  def withCookies(v: Map[String, String]): RequestHeader = copy(cookies = cookies ++ v)
}

object RequestHeader {
  val Default: RequestHeader = RequestHeader(HttpMethods.GET, Uri.EMPTY, Map.empty, Map.empty, Set.empty)
}
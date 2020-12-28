package modux.model.header

import akka.http.javadsl.model.Uri

final case class RequestHeader private(
                                        method: String,
                                        uri: Uri,
                                        headers: Map[String, String],
                                        cookies: Map[String, String],
                                        removeCookies: Set[String]
                                      ) {

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
  val Default: RequestHeader = RequestHeader("GET", Uri.EMPTY, Map.empty, Map.empty, Set.empty)
}
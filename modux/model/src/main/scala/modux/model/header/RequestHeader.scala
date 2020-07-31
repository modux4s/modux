package modux.model.header

trait RequestHeader {
  def method: String
  def uri: String
  def headers: Map[String, String]
  def cookies: Map[String, String]
  def hasHeader(n: String): Boolean
  def removeCookie(n: String): RequestHeader

  def withHeader(n: String, v: String): RequestHeader
  def withHeaders(v: (String, String)*): RequestHeader
  def withHeaders(v: Map[String, String]): RequestHeader

  def withCookie(n: String, v: String): RequestHeader
  def withCookies(v: (String, String)*): RequestHeader
  def withCookies(v: Map[String, String]): RequestHeader
}

object RequestHeader{
  val Default :RequestHeader = RequestHeaderImpl("GET", "/", Map.empty, Map.empty, Set.empty)
}

case class RequestHeaderImpl(
                              method: String,
                              uri: String,
                              headers: Map[String, String],
                              cookies: Map[String, String],
                              removeCookies: Set[String]
                            ) extends RequestHeader {

  override def hasHeader(n: String): Boolean = headers.contains(n)

  override def removeCookie(n: String): RequestHeader = copy(removeCookies = removeCookies - n)

  override def withHeader(n: String, v: String): RequestHeader = copy(headers = headers + (n -> v))
  override def withHeaders(v: (String, String)*): RequestHeader = copy(headers = headers ++ v.toMap)
  override def withHeaders(v: Map[String, String]): RequestHeader = copy(headers = headers ++ v)

  override def withCookie(n: String, v: String): RequestHeader = copy(cookies = cookies + (n -> v))
  override def withCookies(v: (String, String)*): RequestHeader = copy(cookies = cookies ++ v.toMap)
  override def withCookies(v: Map[String, String]): RequestHeader = copy(cookies = cookies ++ v)
}
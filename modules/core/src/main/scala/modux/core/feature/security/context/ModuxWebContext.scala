package modux.core.feature.security.context

import akka.http.scaladsl.model.HttpHeader.ParsingResult.{Error, Ok}
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.{ContentType, HttpHeader, HttpRequest}
import modux.core.feature.security.authorizer.CsrfCookieAuthorizer
import modux.core.feature.security.model.ResponseChanges
import modux.core.feature.security.storage.SessionStorage
import modux.model.header.RequestHeader
import org.pac4j.core.context.{Cookie, WebContext}

import java.util
import java.util.Optional
import java.util.concurrent.atomic.AtomicReference
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

/**
 *
 * The AkkaHttpWebContext is responsible for wrapping an HTTP request and stores changes that are produced by pac4j and
 * need to be applied to an HTTP response.
 */
case class ModuxWebContext(request: RequestHeader, formFields: Seq[(String, String)], sessionStorage: SessionStorage, sessionCookieName: String) extends WebContext {

  private val changes: AtomicReference[ResponseChanges] = new AtomicReference[ResponseChanges](ResponseChanges.empty)

  //Only compute the request cookies once
  private lazy val requestCookies: util.Collection[Cookie] = request.cookies.map { case (k, v) =>
    new Cookie(k, v)
  }.asJavaCollection

  //Request parameters are composed of form fields and the query part of the uri. Stored in a lazy val in order to only compute it once
  private lazy val requestParameters: Map[String, String] = formFields.toMap ++ request.uri.query().toMap.asScala.toMap

  private def newSession(): String = sessionStorage.getOrCreateSessionId(this)

  private val sessionId: AtomicReference[String] = new AtomicReference[String](
    request
      .cookies
      .filter { case (name, _) => name == sessionCookieName }
      .map { case (_, value) => value }
      .find(session => sessionStorage.sessionExists(session))
      .getOrElse(newSession())
  )

  def getSessionID: String = {
    Option(sessionId).map(_.get()).orNull
  }

  private[security] def destroySession(): Boolean = {
    sessionStorage.destroySession(this)
    sessionId.set(newSession())
    true
  }

  private[security] def trackSession(session: String): Boolean = {
    // todo
    sessionStorage.createSessionIfNeeded(session)
    sessionId.set(session)
    true
  }

  override def getRequestCookies: java.util.Collection[Cookie] = requestCookies

  private def toAkkaHttpCookie(cookie: Cookie): HttpCookie = {
    HttpCookie(
      name = cookie.getName,
      value = cookie.getValue,
      expires = None,
      maxAge = if (cookie.getMaxAge < 0) None else Some(cookie.getMaxAge),
      domain = Option(cookie.getDomain),
      path = Option(cookie.getPath),
      secure = cookie.isSecure,
      httpOnly = cookie.isHttpOnly,
      extension = None
    )
  }

  override def addResponseCookie(cookie: Cookie): Unit = {
    val httpCookie: HttpCookie = toAkkaHttpCookie(cookie)
    changes.set(getChanges.copy(cookies = getChanges.cookies ++ Seq(httpCookie)))
  }

  override val getSessionStore: SessionStorage = sessionStorage

  override def getRemoteAddr: String = {
    request.uri.getHost.address()
  }

  override def setResponseHeader(name: String, value: String): Unit = {
    val header: HttpHeader = HttpHeader.parse(name, value) match {
      case Ok(h, _) => h
      case Error(error) => throw new IllegalArgumentException(s"Error parsing http header ${error.formatPretty}")
    }

    // Avoid adding duplicate headers, Pac4J expects to overwrite headers like `Location`
    changes.set(getChanges.copy(headers = header +: getChanges.headers.filter(_.name != name)))
  }

  override def getRequestParameters: java.util.Map[String, Array[String]] = {
    requestParameters.mapValues(Array(_)).asJava
  }

  override def getFullRequestURL: String = {
    request.uri.toString
  }

  override def getServerName: String = {
    request.uri.host.address().split(":")(0)
  }

  override def setResponseContentType(contentType: String): Unit = {
    ContentType.parse(contentType) match {
      case Right(ct) =>
        changes.set(getChanges.copy(contentType = Some(ct)))
      case Left(_) =>
        throw new IllegalArgumentException("Invalid response content type " + contentType)
    }
  }

  override def getPath: String = {
    request.uri.path
  }

  override def getRequestParameter(name: String): Optional[String] = {
    requestParameters.get(name).asJava
  }

  override def getRequestHeader(name: String): Optional[String] = {
    request.headers.find { case (name, _) => name.toLowerCase == name }.map(_._2).asJava
  }

  override def getScheme: String = {
    request.uri.getScheme
  }

  override def isSecure: Boolean = {
    val scheme: String = request.uri.getScheme.toLowerCase
    scheme == "https"
  }

  override def getRequestMethod: String = {
    request.method
  }

  override def getServerPort: Int = {
    request.uri.getPort
  }

  override def setRequestAttribute(name: String, value: scala.AnyRef): Unit = {
    changes.set(getChanges.copy(attributes = getChanges.attributes ++ Map[String, AnyRef](name -> value)))
  }

  override def getRequestAttribute(name: String): Optional[AnyRef] = {
    getChanges.attributes.get(name).asJava
  }

  def getContentType: Option[ContentType] = {
    getChanges.contentType
  }

  def getChanges: ResponseChanges = changes.get()

  def addResponseSessionCookie(): Unit = {
    val cookie = new Cookie(sessionCookieName, getSessionID)
    cookie.setSecure(isSecure)
    cookie.setMaxAge(sessionStorage.sessionLifetime.toSeconds.toInt)
    cookie.setHttpOnly(true)
    cookie.setPath("/")
    addResponseCookie(cookie)
  }

  def addResponseCsrfCookie(): Unit = CsrfCookieAuthorizer(this, Some(sessionStorage.sessionLifetime))
}

object ModuxWebContext {

  final val DEFAULT_COOKIE_NAME = "ModuxPac4jSession"
}

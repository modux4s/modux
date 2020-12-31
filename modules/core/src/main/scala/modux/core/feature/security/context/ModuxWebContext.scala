package modux.core.feature.security.context

import akka.http.scaladsl.model.HttpHeader.ParsingResult.{Error, Ok}
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.{AttributeKey, HttpHeader}
import modux.core.feature.security.authorizer.CsrfCookieAuthorizer
import modux.core.feature.security.storage.SessionStorage
import modux.model.header.Invoke
import org.pac4j.core.context.{Cookie, WebContext}

import java.util
import java.util.Optional
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._
import scala.concurrent.duration.FiniteDuration

/**
 *
 * The AkkaHttpWebContext is responsible for wrapping an HTTP request and stores changes that are produced by pac4j and
 * need to be applied to an HTTP response.
 */
case class ModuxWebContext(invoke: Invoke, formFields: Seq[(String, String)], sessionStorage: SessionStorage, sessionCookieName: String) extends WebContext {

  private val request = invoke.request
  private val finiteDuration: FiniteDuration = FiniteDuration(24, TimeUnit.HOURS)
  //  private val changes: AtomicReference[ResponseChanges] = new AtomicReference[ResponseChanges](ResponseChanges.empty)

  //Only compute the request cookies once

  private lazy val requestCookies: util.Collection[Cookie] = request.cookies.map { c =>
    new Cookie(c.name, c.name)
  }.asJavaCollection

  //Request parameters are composed of form fields and the query part of the uri. Stored in a lazy val in order to only compute it once
  private lazy val requestParameters: Map[String, String] = formFields.toMap ++ request.uri.query().toMap

  private def newSession(): String = {
    sessionStorage.getOrCreateSessionId(this)
  }

  private val _sessionId: AtomicReference[String] = new AtomicReference[String]()
  _sessionId.set(
    request
      .cookies
      .filter(x => x.name == sessionCookieName)
      .map(_.value)
      .find(session => sessionStorage.existsSession(session))
      .getOrElse(newSession())
  )

  def sessionId: String = _sessionId.get()

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
    invoke.responseHeader.withCookies(List(toAkkaHttpCookie(cookie)))
  }

  override val getSessionStore: SessionStorage = sessionStorage

  override def getRemoteAddr: String = {
    request.getUri().getHost.address()
  }

  override def setResponseHeader(name: String, value: String): Unit = {
    val header: HttpHeader = HttpHeader.parse(name, value) match {
      case Ok(h, _) => h
      case Error(error) => throw new IllegalArgumentException(s"Error parsing http header ${error.formatPretty}")
    }

    invoke.responseHeader.withHeader(header)
  }

  override def getRequestParameters: java.util.Map[String, Array[String]] = {
    requestParameters.mapValues(Array(_)).asJava
  }

  override def getFullRequestURL: String = {
    request.uri.toString
  }

  override def getServerName: String = {
    request.getUri().host.address().split(":")(0)
  }

  override def setResponseContentType(contentType: String): Unit = {
    invoke.responseHeader.withContentType(contentType)
  }

  override def getPath: String = {
    request.getUri().path
  }

  override def getRequestParameter(name: String): Optional[String] = {
    requestParameters.get(name).asJava
  }

  override def getRequestHeader(name: String): Optional[String] = {
    request.headers.find(_.name().toLowerCase() == name.toLowerCase).map(_.value).asJava
  }

  override def getScheme: String = {
    request.getUri().getScheme
  }

  override def isSecure: Boolean = {
    val scheme: String = request.getUri().getScheme.toLowerCase
    scheme == "https"
  }

  override def getRequestMethod: String = {
    request.method.value
  }

  override def getServerPort: Int = {
    request.getUri().getPort
  }

  override def setRequestAttribute(name: String, value: scala.AnyRef): Unit = {
    invoke.responseHeader.withAttributes(Map[String, AnyRef](name -> value))
  }

  override def getRequestAttribute(name: String): Optional[AnyRef] = {
    invoke.request.getAttribute(AttributeKey[AnyRef](name))
  }

  def getResponseSessionCookie: HttpCookie = {
    val cookie = new Cookie(sessionCookieName, sessionId)
    cookie.setSecure(isSecure)
    cookie.setMaxAge(finiteDuration.toSeconds.toInt)
    cookie.setHttpOnly(true)
    cookie.setPath("/")
    toAkkaHttpCookie(cookie)
  }

  def addResponseCsrfCookie(): Unit = CsrfCookieAuthorizer(this, Some(finiteDuration))
}

object ModuxWebContext {

  final val DEFAULT_COOKIE_NAME = "ModuxPac4jSession"
}

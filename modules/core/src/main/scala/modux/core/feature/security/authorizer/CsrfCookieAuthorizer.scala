package modux.core.feature.security.authorizer

import modux.core.feature.security.context.ModuxWebContext
import org.pac4j.core.context.Cookie
import org.pac4j.core.util.Pac4jConstants

import java.util.UUID
import scala.concurrent.duration.FiniteDuration

object CsrfCookieAuthorizer {
  val CookiePath = "/"

  def apply(context: ModuxWebContext, maxAge: Option[FiniteDuration]): ModuxWebContext = {
    val token = UUID.randomUUID.toString
    val cookie = new Cookie(Pac4jConstants.CSRF_TOKEN, token)
    cookie.setPath(CookiePath)

    maxAge.map(_.toSeconds.toInt).foreach {
      cookie.setMaxAge
    }

    context.setRequestAttribute(Pac4jConstants.CSRF_TOKEN, token)
    context.getSessionStore.set(context, Pac4jConstants.CSRF_TOKEN, token)
    context.addResponseCookie(cookie)

    context
  }
}

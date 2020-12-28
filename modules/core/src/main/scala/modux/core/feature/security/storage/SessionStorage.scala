package modux.core.feature.security.storage

import modux.core.feature.security.context.ModuxWebContext
import org.pac4j.core.context.session.SessionStore

import scala.concurrent.duration.FiniteDuration

trait SessionStorage extends SessionStore[ModuxWebContext] {
  val sessionLifetime: FiniteDuration

  def createSessionIfNeeded(sessionKey: String): Boolean

  def sessionExists(sessionKey: String): Boolean
}

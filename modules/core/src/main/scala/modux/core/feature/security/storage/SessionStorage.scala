package modux.core.feature.security.storage

import modux.core.feature.security.context.ModuxWebContext
import org.pac4j.core.context.session.SessionStore

trait SessionStorage extends SessionStore[ModuxWebContext] {
  def existsSession(sessionId: String): Boolean
}

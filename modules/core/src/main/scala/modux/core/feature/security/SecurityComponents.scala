package modux.core.feature.security

import modux.core.feature.security.context.ModuxWebContext
import modux.core.feature.security.storage.{InMemory, SessionStorage}
import org.pac4j.core.config.Config

trait SecurityComponents {
  def config: Config

  def sessionStorage: SessionStorage = InMemory()

  def sessionCookieName: String = ModuxWebContext.DEFAULT_COOKIE_NAME
}

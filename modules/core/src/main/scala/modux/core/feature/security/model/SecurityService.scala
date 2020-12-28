package modux.core.feature.security.model

import akka.NotUsed
import modux.core.feature.security.context.ModuxWebContext
import modux.model.service.Call
import org.pac4j.core.util.Pac4jConstants

trait SecurityService {

  def callback(
                defaultUrl: String,
                saveInSession: Boolean = true,
                multiProfile: Boolean = true,
                defaultClient: Option[String] = None,
                enforceFormEncoding: Boolean = false,
                existingContext: Option[ModuxWebContext] = None,
                setCsrfCookie: Boolean = true
              ): () => Call[Unit, NotUsed]

  def logout(): Call[Unit, NotUsed]

  def secure[A, B](clients: String = null, multiProfile: Boolean = true, authorizers: String = "")(inner: => Call[A, B]): Call[A, B] = {
    withAuthentication(clients, multiProfile, authorizers)(_ => inner)
  }

  def withAuthentication[A, B](clients: String = null, multiProfile: Boolean = true, authorizers: String = "")(inner: AuthenticatedRequest => Call[A, B]): Call[A, B]
}

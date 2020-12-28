package modux.core.feature.security.storage

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import modux.core.feature.security.context.ModuxWebContext
import org.pac4j.core.context.session.SessionStore

import java.time.Duration
import java.util.{Optional, UUID}
import java.util.concurrent.TimeUnit
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.concurrent.duration.FiniteDuration

final case class InMemory() extends SessionStorage {

  private val cache: Cache[String, Map[String, AnyRef]] = Caffeine
    .newBuilder()
    .expireAfterAccess(Duration.ofMinutes(5))
    .build()

  override val sessionLifetime: FiniteDuration = FiniteDuration(8, TimeUnit.HOURS)

  override def createSessionIfNeeded(sessionKey: String): Boolean = {
    cache.get(sessionKey, _ => Map.empty[String, AnyRef])
    true
  }

  override def sessionExists(sessionKey: String): Boolean = {
    Option(cache.getIfPresent(sessionKey)).isDefined
  }

  override def getOrCreateSessionId(context: ModuxWebContext): String = {
    Option(context.getSessionID).getOrElse(UUID.randomUUID().toString)
  }

  override def get(context: ModuxWebContext, key: String): Optional[AnyRef] = {
    createSessionIfNeeded(context.getSessionID)
    Option(cache.getIfPresent(context.getSessionID)).flatMap(_.get(key)).asJava
  }

  override def set(context: ModuxWebContext, key: String, value: AnyRef): Unit = {
    Option(cache.getIfPresent(context.getSessionID))
      .foreach { current =>
        cache.put(context.getSessionID, current + (key -> value))
      }
  }

  override def destroySession(context: ModuxWebContext): Boolean = {
    cache.invalidate(context.getSessionID)
    true
  }

  override def getTrackableSession(context: ModuxWebContext): Optional[_] = Optional.empty()

  override def buildFromTrackableSession(context: ModuxWebContext, trackableSession: Any): Optional[SessionStore[ModuxWebContext]] = Optional.empty()

  override def renewSession(context: ModuxWebContext): Boolean = {
    cache.put(context.getSessionID, cache.getIfPresent(context.getSessionID))
    true
  }
}

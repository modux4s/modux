package modux.core.feature.security.storage

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import modux.core.feature.security.context.ModuxWebContext
import org.pac4j.core.context.session.SessionStore

import java.time.Duration
import java.util.{Optional, UUID}
import scala.compat.java8.OptionConverters.RichOptionForJava8

final case class InMemory() extends SessionStorage {

  private val cache: Cache[String, Map[String, AnyRef]] = Caffeine
    .newBuilder()
    .expireAfterAccess(Duration.ofMinutes(5))
    .build()

  override def getOrCreateSessionId(context: ModuxWebContext): String = {
    val id: String = Option(context.sessionId).getOrElse(UUID.randomUUID().toString)
    cache.get(id, _ => Map.empty[String, AnyRef])
    id
  }

  override def get(context: ModuxWebContext, key: String): Optional[AnyRef] = {
//    Option(cache.getIfPresent(context.sessionId)).flatMap(_.get(key)).asJava
    {
      for{
        id <- Option(context.sessionId)
        ref <- Option(cache.getIfPresent(id)).flatMap(_.get(key))
      } yield ref
    }.asJava
  }

  override def set(context: ModuxWebContext, key: String, value: AnyRef): Unit = {
    val sessionId: String = context.sessionId
    Option(cache.getIfPresent(sessionId))
      .foreach { current =>
        cache.put(sessionId, current + (key -> value))
      }
  }

  override def destroySession(context: ModuxWebContext): Boolean = {
    cache.invalidate(context.sessionId)
    true
  }

  override def getTrackableSession(context: ModuxWebContext): Optional[_] = Optional.empty()

  override def buildFromTrackableSession(context: ModuxWebContext, trackableSession: Any): Optional[SessionStore[ModuxWebContext]] = Optional.empty()

  override def renewSession(context: ModuxWebContext): Boolean = {
    val sessionId: String = context.sessionId
    cache.put(sessionId, cache.getIfPresent(sessionId))
    true
  }

  override def existsSession(sessionId: String): Boolean = Option(cache.getIfPresent(sessionId)).isDefined
}

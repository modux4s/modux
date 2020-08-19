package modux.core.feature

import java.util.concurrent.TimeUnit

import akka.pattern.CircuitBreaker
import modux.model.context.Context
import modux.model.dsl.{ResponseAsFalseFail, RestEntryExtension}

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Try}

final case class CircuitBreakExtension(
                                        maxFailure: Int,
                                        callTimeout: FiniteDuration,
                                        resetTimeout: FiniteDuration,
                                      )(implicit context: Context) extends RestEntryExtension {
  private val cb: CircuitBreaker = CircuitBreaker(context.classicActorSystem.scheduler, maxFailure, callTimeout, resetTimeout)

  override def call[T](f: => Future[T]): Future[T] = {
    cb.withCircuitBreaker(
      f,
      (x: Try[T]) => {
        x match {
          case Failure(exception) =>
            exception match {
              case _: ResponseAsFalseFail => false
              case _ => true
            }
          case _ => false
        }
      })
  }
}

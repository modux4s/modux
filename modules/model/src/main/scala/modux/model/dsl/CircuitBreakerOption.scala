package modux.model.dsl

import scala.concurrent.duration.FiniteDuration

case class CircuitBreakerOption(
                                 maxFailures: Option[Int],
                                 callTimeout: Option[FiniteDuration],
                                 resetTimeout: Option[FiniteDuration]
                               )

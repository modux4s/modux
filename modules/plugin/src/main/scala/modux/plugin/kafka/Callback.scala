package modux.plugin.kafka

import sbt.internal.util.ManagedLogger

trait Callback {
  def onComplete(): Unit

  def onError(): Unit

  def logger: ManagedLogger
}

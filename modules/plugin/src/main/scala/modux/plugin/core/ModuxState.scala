package modux.plugin.core

import java.util.concurrent.atomic.AtomicReference

import sbt._

sealed trait ModuxState {
  def initialized: Boolean

  def isContinuous: Boolean

  def serverReloader: ServerReloader

}

case object Empty extends ModuxState {

  override def initialized: Boolean = false

  override def isContinuous: Boolean = false

  override def serverReloader: ServerReloader = throw new NoSuchElementException("serverReloader")
}

case class InProgress(state: State, serverReloader: ServerReloader) extends ModuxState {
  lazy val isContinuousMode: Boolean = {
//    state.get(Watched.ContinuousWatchService).isDefined || state.get(Watched.ContinuousEventMonitor).isDefined
//    println(state.interactive)
//    state.definedCommands.exists(_.nameOption.contains("~"))
    true
  }

  override def initialized: Boolean = true

  override def isContinuous: Boolean = isContinuousMode
}

private[plugin] object ModuxState {
  private val state: AtomicReference[ModuxState] = new AtomicReference(Empty)

  def update(moduxState: ModuxState): ModuxState = {
    state.set(moduxState)
    moduxState
  }

  def get: ModuxState = state.get()

  def clean(): Unit = state.set(Empty)
}

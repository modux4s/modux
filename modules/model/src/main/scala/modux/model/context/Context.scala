package modux.model.context

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.stream.Materializer
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

trait Context {

  lazy val self: Context = this
  lazy val materializer: Materializer = Materializer(actorSystem)
  lazy val actorSystem: ActorSystem[Nothing] = classicActorSystem.toTyped

  def applicationName: String

  def config: Config

  def classicActorSystem: ClassicActorSystem

  def executionContext: ExecutionContext

  def applicationLoader: ClassLoader

  def contextThread[T](f: => T): T = {
    val thread: Thread = Thread.currentThread
    val oldLoader: ClassLoader = thread.getContextClassLoader
    try {
      thread.setContextClassLoader(applicationLoader)
      f
    } finally {
      thread.setContextClassLoader(oldLoader)
    }
  }

}

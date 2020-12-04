package modux.model.context

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem => ClassicActorSystem}
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

trait Context {
  lazy val classicActorSystem: ClassicActorSystem = actorSystem.toClassic
  lazy val self: Context = this

  val loader: ClassLoader
  val config: Config
  val actorSystem: ActorSystem[Nothing]
  val executionContext: ExecutionContext
}

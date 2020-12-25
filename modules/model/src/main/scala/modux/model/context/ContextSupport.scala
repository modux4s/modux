package modux.model.context

import akka.actor.typed.ActorSystem
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

trait ContextSupport {
  protected implicit lazy val actorSystem: ActorSystem[Nothing] = context.actorSystem
  protected implicit lazy val ec: ExecutionContext = context.executionContext
  protected implicit lazy val mat: Materializer = Materializer(actorSystem)
  protected implicit val _ctx: Context = context

  def context: Context
}

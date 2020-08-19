package modux.core.feature

import akka.pattern.RetrySupport
import modux.model.context.Context
import modux.model.dsl.RestEntryExtension

import scala.concurrent.{ExecutionContext, Future}

final case class RetryExtension(attempts: Int )(implicit context: Context) extends RestEntryExtension {

  private implicit val ec: ExecutionContext = context.executionContext

  override def call[T](f: => Future[T]): Future[T] = {
    RetrySupport.retry(() => f, attempts)
  }
}

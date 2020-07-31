package modux.common

import scala.concurrent.Future

trait FutureImplicits {
  protected implicit def toFuture[T](d: T): Future[T] = Future.successful(d)
}

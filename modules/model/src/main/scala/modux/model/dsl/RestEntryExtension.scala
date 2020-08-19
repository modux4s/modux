package modux.model.dsl

import scala.concurrent.Future

trait RestEntryExtension {
  def call[T](f: => Future[T]): Future[T]
}
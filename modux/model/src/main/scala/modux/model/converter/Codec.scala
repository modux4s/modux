package modux.model.converter

import scala.concurrent.Future

trait Codec[A, B] {
  def apply(data: A): Future[B]
}

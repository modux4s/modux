package modux.model

import modux.model.header.Invoke

import scala.concurrent.Future

package object service {

  type Call[IN, OUT] = (IN, Invoke) => Future[OUT]
}

package modux.model

import modux.model.header.{RequestHeader, ResponseHeader}

import scala.concurrent.Future

package object service {
  type Call[IN, OUT] = (IN, RequestHeader, ResponseHeader) => Future[(OUT, ResponseHeader)]
}

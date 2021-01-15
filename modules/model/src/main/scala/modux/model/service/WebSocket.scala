package modux.model.service

import akka.http.scaladsl.util.FastFuture
import modux.model.ws.WSEvent

import scala.concurrent.ExecutionContext

object WebSocket {
  def apply[IN, OUT](f: WSEvent[IN, OUT] => Unit)(implicit ec: ExecutionContext): Call[WSEvent[IN, OUT], Unit] = {
    (input: WSEvent[IN, OUT], _) => FastFuture.successful(f(input))
  }
}

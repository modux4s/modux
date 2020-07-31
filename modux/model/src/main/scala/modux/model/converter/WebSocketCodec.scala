package modux.model.converter

import akka.http.scaladsl.model.ws.Message

import scala.concurrent.Future

trait WebSocketCodec[A, B] {
  def encoder: Codec[Message, A]
  def decoder: Codec[B, Message]
  def encode(message: Message): Future[A] = encoder(message)
  def decode(message: B): Future[Message] = decoder(message)
}

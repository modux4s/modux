package modux.model.ws


import akka.actor.typed.ActorRef
import akka.actor.{ActorRef => CActorRef}
import akka.http.scaladsl.model.ws.Message


//************** commands **************//
sealed trait WSCommand

final case class PushMessage(message: Message) extends WSCommand

final case class SendMessage[OUT](message: OUT) extends WSCommand

final case class CloseConnection(forced: Boolean, id: String) extends WSCommand

final case class HandleOut(actorRef: CActorRef) extends WSCommand

final case class ConnectionRef[OUT] private(connectionID: String, actorRef: ActorRef[WSCommand]) {
  def id: String = connectionID

  def sendMessage(message: OUT): Unit = {
    actorRef ! SendMessage(message)
  }
}

sealed trait WSEvent[IN, OUT]

final case class OnOpenConnection[IN, OUT](connection: ConnectionRef[OUT]) extends WSEvent[IN, OUT]

final case class OnCloseConnection[IN, OUT](connectionID: String) extends WSEvent[IN, OUT]

final case class OnMessage[IN, OUT](connection: ConnectionRef[OUT], message: IN) extends WSEvent[IN, OUT]
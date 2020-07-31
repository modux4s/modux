package mserver.example

import modux.model.context.Context
import modux.model.service.{Call, WebSocket}
import modux.model.ws.{OnCloseConnection, OnMessage, OnOpenConnection, WSEvent}

case class UserServiceImpl(context: Context) extends UserService {

  override def getUser(id: String, age: Option[Int]): Call[Unit, Option[Pepe]] = Call {
    logger.info(id)
    Option(Pepe(id, age.getOrElse(0), "4"))
    //    NotFound("hola!!")


  }

  override def postUser(): Call[User, Unit] = { usr =>

    println(usr)
  }

  def websocket: Call[WSEvent[String, Pepe], Unit] = WebSocket {
    case OnOpenConnection(connection) => logger.info(s"Connection opened ${connection.id}")
    case OnCloseConnection(connectionID) => logger.info(s"Connection closed $connectionID")
    case OnMessage(connection, message) => connection.sendMessage(Pepe(message, 29, "0"))
  }
}

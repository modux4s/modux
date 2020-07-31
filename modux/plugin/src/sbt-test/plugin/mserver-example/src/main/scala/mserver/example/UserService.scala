package mserver.example

import akka.NotUsed
import com.typesafe.scalalogging.LazyLogging
import modux.core.api.Service
import modux.macros.serializer.SerializationSupport
import modux.macros.serializer.codec.Codec
import modux.model.ServiceDescriptor
import modux.model.converter.WebSocketCodec
import modux.model.service.Call
import modux.model.ws.WSEvent
import modux.macros.serializer.SerializationSupport.DefaultCodecRegistry

trait UserService extends Service with SerializationSupport with LazyLogging {

  private implicit val codec: WebSocketCodec[String, Pepe] = websocketCodec[String, Pepe]
  private implicit val userCodec: Codec[User] = codify[User]
  private implicit val pepeCodec: Codec[Pepe] = codify[Pepe]
  private implicit val errorCodec: Codec[ErrorReport] = codify[ErrorReport]

  def getUser(id: String, age: Option[Int]): Call[Unit, Option[Pepe]]

  def postUser(): Call[User, Unit]

  def websocket: Call[WSEvent[String, Pepe], Unit]

  override def serviceDescriptor: ServiceDescriptor = (
    namedAs("user-service-1")
      .withCalls(

        statics("app/home", "public"),

        post("/user", postUser _)
          expect item[User]
          response(200 summary "Returns a user", 500 summary "internal error"),

        get("/user/{id}?age", getUser _)
          summary "Obtain's a user"
          describe path("id") as "User's  identifier" withExamples "sample1" -> "pepe"
          response(200 summary "Returns a user", 404 summary "Not dound"),

        named("ws", websocket _)
      )
    )
}

package modux.macros.serializer.websocket

import scala.reflect.macros.blackbox

object JsonWebSocketCodecImpl {

  def websocket[A: c.WeakTypeTag, B: c.WeakTypeTag](c: blackbox.Context): c.Tree = {
    def decodeEntity(typeA: String): String = {
      s"""
         |{
         |  import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
         |  import scala.concurrent.{ExecutionContext, Future}
         |  import com.fasterxml.jackson.databind.ObjectMapper
         |
         |  new modux.model.converter.Codec[$typeA, Message]{
         |    private val jsonMapper: ObjectMapper = modux.macros.serializer.codec.providers.impl.CodecUtils.createJsonMapper()
         |
         |    def apply(data: $typeA): Future[Message] = Future{
         |      TextMessage.Strict(jsonMapper.writeValueAsString(data))
         |    }
         |  }
         |}
         |""".stripMargin
    }

    def encodeToEntity(typeB: String): String = {
      if (typeB.contains("String")) {
        s"""
           |{
           |  import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
           |  import scala.concurrent.{ExecutionContext, Future}
           |
           |  new modux.model.converter.Codec[Message, $typeB]{
           |
           |    def apply(data: Message): Future[$typeB] = data match {
           |      case message: TextMessage =>
           |        message match {
           |          case TextMessage.Strict(text) => Future(text)
           |          case _ => throw new RuntimeException("No supported BinaryMessage")
           |        }
           |      case _: BinaryMessage => throw new RuntimeException("No supported BinaryMessage")
           |    }
           |  }
           |}
           |""".stripMargin
      } else {
        s"""
           |{
           |  import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
           |  import scala.concurrent.{ExecutionContext, Future}
           |  import com.fasterxml.jackson.databind.ObjectMapper
           |
           |  new modux.model.converter.Codec[Message, $typeB]{
           |    private val jsonMapper: ObjectMapper = modux.macros.serializer.codec.providers.impl.CodecUtils.createJsonMapper()
           |    private def readValue[A](str: String)(implicit mf: Manifest[A]): A = jsonMapper.readValue(str, mf.runtimeClass).asInstanceOf[A]
           |
           |    def apply(data: Message): Future[$typeB] = data match {
           |      case message: TextMessage =>
           |        message match {
           |          case TextMessage.Strict(text) =>
           |            Future(readValue[$typeB](text))
           |          case _ =>
           |            throw new RuntimeException("No supported BinaryMessage")
           |        }
           |      case _: BinaryMessage => throw new RuntimeException("No supported BinaryMessage")
           |    }
           |  }
           |}
           |""".stripMargin
      }
    }

    implicit def asStr[T](d: T): String = d.toString

    import c.universe.{weakTypeTag => t}

    val typeA: c.universe.WeakTypeTag[A] = t[A]
    val typeB: c.universe.WeakTypeTag[B] = t[B]

    val tpeA: c.universe.Type = typeA.tpe
    val tpeB: c.universe.Type = typeB.tpe

    val impl: String =
      s"""
         |{
         |  import akka.http.scaladsl.model.ws.Message
         |  import modux.model.converter.{WebSocketCodec, Codec}
         |
         |  new WebSocketCodec[$tpeA, $tpeB]{
         |    def encoder: Codec[Message, $tpeA] = ${encodeToEntity(tpeA)}
         |    def decoder: Codec[$tpeB, Message] = ${decodeEntity(tpeB)}
         |  }
         |}
         |""".stripMargin

    c.parse(impl)
  }
}

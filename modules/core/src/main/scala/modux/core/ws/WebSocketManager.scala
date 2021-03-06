package modux.core.ws

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.{ActorRef => CActor}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity, EntityRef, EntityTypeKey}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{CompletionStrategy, OverflowStrategy}
import akka.{Done, NotUsed}
import modux.model.converter.WebSocketCodec
import modux.model.header.Invoke
import modux.model.service.Call
import modux.model.ws._
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object WebSocketManager {

  def apply[IN, OUT](name: String, call: Call[WSEvent[IN, OUT], Unit])(implicit mapper: WebSocketCodec[IN, OUT], as: ActorSystem[Nothing]): WebSocketManager[IN, OUT] = {
    new WebSocketManager(name, call)
  }
}

final class WebSocketManager[IN, OUT](name: String, call: Call[WSEvent[IN, OUT], Unit])(implicit mapper: WebSocketCodec[IN, OUT], as: ActorSystem[Nothing]) {

  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private lazy implicit val ec: ExecutionContext = as.executionContext
  private lazy val sharding: ClusterSharding = ClusterSharding(as)
  private lazy val TypeKey: EntityTypeKey[WSCommand] = EntityTypeKey[WSCommand](s"$name-pusher")

  sharding.init(Entity(TypeKey)(createBehavior = entityContext => initActor(entityContext.entityId)))

  private def initActor(id: String): Behavior[WSCommand] = Behaviors.setup { ctx =>

    Behaviors.receiveMessagePartial {
      case HandleOut(actorRef, requestHeader) => initializedActor(id, actorRef, requestHeader)
    }
  }

  private def initializedActor(id: String, outActor: CActor, invoke: Invoke): Behavior[WSCommand] = Behaviors.setup { ctx =>

    val connectionRef: ConnectionRef[OUT] = ConnectionRef(id, ctx.self)

    call(OnOpenConnection(connectionRef), invoke)

    Behaviors.receiveMessagePartial {
      case x: PushMessage =>

        mapper.encode(x.message).onComplete {
          case Failure(exception) => logger.error(exception.getLocalizedMessage, exception)
          case Success(messageIn) => call(OnMessage(connectionRef, messageIn), invoke)
        }

        Behaviors.same

      case x: SendMessage[OUT] =>

        mapper.decode(x.message).onComplete {
          case Failure(exception) => logger.error(exception.getLocalizedMessage, exception)
          case Success(value) => outActor ! value
        }

        Behaviors.same

      case x: CloseConnection =>

        call(OnCloseConnection(id), invoke)

        if (x.forced) outActor ! Done

        Behaviors.stopped
    }
  }

  def listen(request: Invoke): Flow[Message, Message, NotUsed] = {

    val id: String = UUID.randomUUID().toString
    val actorRef: EntityRef[WSCommand] = sharding.entityRefFor(TypeKey, id)

    val inbound: Sink[Message, Any] = Sink.foreach {
      case message: TextMessage =>
        message match {
          case x: TextMessage.Strict => actorRef ! PushMessage(x)
          case _ => logger.warn("TextMessage.Streamed not supported")
        }
      case _: BinaryMessage => logger.warn("BinaryMessage not supported")
    }

    val c: PartialFunction[Any, CompletionStrategy] = {
      case Done => CompletionStrategy.immediately
    }

    val source: Source[TextMessage, CActor] = Source.actorRef[TextMessage](c, PartialFunction.empty, 100, OverflowStrategy.dropHead)

    Flow.fromSinkAndSourceMat(inbound, source)((_, outboundMat) => {

      actorRef ! HandleOut(outboundMat, request)

      NotUsed
    })
      .watchTermination() { (_, fut) =>

        fut.onComplete { _ =>
          actorRef ! CloseConnection(forced = false, id)
        }

        NotUsed
      }
  }
}

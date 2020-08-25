package modux.core.ws

import java.util.UUID

import akka.NotUsed
import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.ws.Message
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source, SourceQueueWithComplete}
import modux.model.converter.Mapper
import modux.model.header.RequestHeader
import modux.model.service.CallSource
import modux.model.ws._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.parallel.mutable.ParMap
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class WebSocketManagerBak[INPUT, OUTPUT](
                                       call: CallSource[WSOpenConnection, Option[ActorRef[WSCommand]]]
                                     )
                                        (
                                       implicit ec: ExecutionContext,
                                       inMapper: Mapper[INPUT, Message],
                                       outMapper: Mapper[OUTPUT, Message]
                                     ) {

  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private final val connections: ParMap[UUID, WSConnection[OUTPUT]] = ParMap.empty

  def deleteConnection(id: UUID): Unit = {
    connections.get(id).foreach(_.close())
    connections -= id
  }

  def listen(requestHeader: RequestHeader): Flow[Message, Message, NotUsed] = {
    val id: UUID = UUID.randomUUID()
    val outbound: Source[Message, SourceQueueWithComplete[Message]] = Source.queue[Message](16, OverflowStrategy.backpressure)

    val inbound: Sink[Message, Any] = Sink.foreach { m =>

      connections.get(id) match {
        case Some(conn) =>
          inMapper.from(m).onComplete {
            case Success(x) =>
              call
                .handle(OnMessage(x, conn), requestHeader)
                .onComplete {
                  case Failure(exception) => logger.error(exception.getLocalizedMessage, exception)
                  case Success((_, action)) =>

                    action match {
                      case WSAction.Close => deleteConnection(conn.id)
                      case _ =>
                    }
                }
            case _ => logger.warn(s"Cant handle $m")
          }

        case None => logger.warn(s"Connection $id not available")
      }
    }

    Flow.fromSinkAndSourceMat(inbound, outbound)((_, outboundMat) => {

      // build put buffer messages
      val connection: WSConnection[OUTPUT] = WSConnectionImpl(id, this, outboundMat)
      connections.put(id, connection)

      call.handle(OpenConnection(connection), requestHeader)

      NotUsed
    })
      .watchTermination() { case (_, fut) =>

        // onClose event
        fut.foreach { _ =>
          connections.get(id).foreach { conn =>
            call.handle(OnClose(conn), requestHeader).foreach { _ =>
              deleteConnection(conn.id)
            }
          }
        }

        NotUsed
      }
  }
}

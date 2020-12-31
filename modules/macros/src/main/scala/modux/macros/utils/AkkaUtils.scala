package modux.macros.utils

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.{Directive0, Directive1, Directives, RequestContext}
import modux.model.header._
import modux.model.instruction.impl.response.ResponseInstruction
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object AkkaUtils extends Directives {

//  import akka.http.scaladsl.server.Directives.mapResponse
  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def check[T](f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    Try(f) match {
      case Failure(exception) => Future.failed(exception)
      case Success(value) => value
    }
  }

  def createInvoke(requestContext: RequestContext): Invoke = Invoke(requestContext, Nil, ResponseHeader())

  def mapResponse(invoke: Invoke): Directive0 = {
    mapResponse{response =>
      ResponseInstruction(response, invoke.responseHeader.getInstructions)
    }
  }

  def mapRequest(req: HttpRequest): RequestHeader = {

    RequestHeader(
      req.method,
      req.getUri(),
      req.headers.map(x => x.name() -> x.value()).toMap,
      req.cookies.map(x => x.name -> x.value).toMap,
      Set.empty
    )
  }
}

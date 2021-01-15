package modux.model.header

import akka.http.scaladsl.marshalling.{PredefinedToResponseMarshallers, ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, RequestEntity}
import akka.http.scaladsl.server.RequestContext
import akka.stream.Materializer
import com.typesafe.scalalogging.LazyLogging
import modux.model.dsl.ResponseAsFalseFail
import modux.model.instruction.impl.response.{ResponseInstruction, SetStatus}
import org.pac4j.core.profile.UserProfile

import scala.concurrent.{ExecutionContextExecutor, Future}

final case class Invoke(private[modux] val requestContext: RequestContext, private[modux] val profiles: List[UserProfile], private[modux] val responseHeader: ResponseHeader) {

  private[modux] def request: HttpRequest = requestContext.request

  private[modux] implicit def materializer: Materializer = requestContext.materializer

  private[modux] implicit def executionContext: ExecutionContextExecutor = requestContext.executionContext

  private[modux] def entity: RequestEntity = request.entity

  private[modux] def withProfiles(items: List[UserProfile]): Invoke = {
    Invoke(requestContext = requestContext, profiles = items, responseHeader = responseHeader)
  }

  def withResponseHeader(item: ResponseHeader): Invoke = Invoke(requestContext = requestContext, profiles = profiles, responseHeader = item)

  def mapResponse(f: ResponseHeader => ResponseHeader): Invoke = withResponseHeader(f(responseHeader))

  def isValid: Boolean = responseHeader.getInstructions.forall {
    case SetStatus(statusCode) => statusCode < 300
    case _ => true
  }
}

object Invoke {

  def fail[T](invoke: Invoke, response: HttpResponse = HttpResponse()): Future[T] = {
    val responseHeader: ResponseHeader = invoke.responseHeader
    Future.failed(
      ResponseAsFalseFail(
        new ToResponseMarshallable {
          override type T = HttpResponse

          override def value: HttpResponse = ResponseInstruction(response, responseHeader.getInstructions)

          override implicit def marshaller: ToResponseMarshaller[HttpResponse] = PredefinedToResponseMarshallers.fromResponse
        }
      )
    )
  }
}
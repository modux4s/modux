package modux.macros.utils

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.headers.{Cookie, HttpCookie, `Set-Cookie`}
import akka.http.scaladsl.model.{ContentTypes, HttpHeader, HttpRequest}
import akka.http.scaladsl.server.{Directive0, Directives}
import modux.model.header._
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object AkkaUtils extends Directives {

  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def check[T](f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    Try(f) match {
      case Failure(exception) => Future.failed(exception)
      case Success(value) => value
    }
  }

  def mapRequest(req: HttpRequest): RequestHeader = {
    RequestHeaderImpl(
      req.method.value,
      req.uri.toString(),
      req.headers.map(x => x.name() -> x.value()).toMap,
      req.cookies.map(x => x.name -> x.value).toMap,
      Set.empty
    )
  }

  def mapResponse(responseHeader: ResponseHeader): Directive0 = {
    mapResponse { response =>

      val header: List[HttpHeader] = responseHeader.headers.flatMap { case (k, v) =>
        HttpHeader.parse(k, v) match {
          case ParsingResult.Ok(h, _) => Option(h)
          case x =>
            x.errors.foreach(y => logger.info(y.summary, y.detail))
            None
        }
      }.toList

      val extraHeaders: List[`Set-Cookie`] = responseHeader.cookies.values.map(x => `Set-Cookie`(x)).toList

      response
        .withHeaders(header ++ extraHeaders)
        .withStatus(responseHeader.status)
        .mapEntity { entity =>
          responseHeader.contentAs match {
            case Default => entity
            case Json => entity.withContentType(ContentTypes.`application/json`)
            case Xml => entity.withContentType(ContentTypes.`text/xml(UTF-8)`)
            case TextPlain => entity.withContentType(ContentTypes.`text/plain(UTF-8)`)
            case Html => entity.withContentType(ContentTypes.`text/html(UTF-8)`)
            case Custom(ct) => entity.withContentType(ct)
          }
        }
    }
  }
}

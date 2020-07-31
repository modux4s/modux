package modux.macros.utils

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.{ContentTypes, HttpHeader, HttpRequest}
import akka.http.scaladsl.server.{Directive0, Directives}
import modux.model.header._

object AkkaUtils extends Directives {

  def apply(req: HttpRequest): RequestHeader = {
    RequestHeaderImpl(
      req.method.value,
      req.uri.toString(),
      req.headers.map(x => x.lowercaseName() -> x.value()).toMap,
      req.cookies.map(x => x.name -> x.value).toMap,
      Set.empty
    )
  }

  def render(responseHeader: ResponseHeader): Directive0 = {
    mapResponse { response =>

      val header: List[HttpHeader] = responseHeader.headers.flatMap { case (k, v) =>
        HttpHeader.parse(k, v) match {
          case ParsingResult.Ok(h, _) => Option(h)
          case _ => None
        }
      }.toList

      response
        .withHeaders(header)
        .withStatus(responseHeader.status)
        .mapEntity { entity =>
          responseHeader.contentAs match {
            case Default => entity
            case Json => entity.withContentType(ContentTypes.`application/json`)
            case Xml => entity.withContentType(ContentTypes.`text/xml(UTF-8)`)
            case TextPlain => entity.withContentType(ContentTypes.`text/plain(UTF-8)`)
            case Html => entity.withContentType(ContentTypes.`text/html(UTF-8)`)
          }
        }
    }
  }
}

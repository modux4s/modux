package modux.core.feature.security.model

import akka.http.scaladsl.model.headers.{HttpCookie, `Set-Cookie`}
import akka.http.scaladsl.model.{ContentType, HttpHeader}
import modux.model.header.ResponseHeader

//This class is where all the HTTP response changes are stored so that they can later be applied to an HTTP Request
final case class ResponseChanges(headers: List[HttpHeader],
                                 contentType: Option[ContentType],
                                 content: String,
                                 cookies: List[HttpCookie],
                                 attributes: Map[String, AnyRef])

object ResponseChanges {
  def empty: ResponseChanges = ResponseChanges(List.empty, None, "", List.empty, Map.empty)

  def apply(changes: ResponseChanges, httpResponse: ResponseHeader): ResponseHeader = {

    httpResponse
      .withHeaders(changes.headers)
      .withHeaders(changes.cookies.map(x => `Set-Cookie`(x)))
    //      .withAttributes(changes.attributes.map{case (k, v)=>  AttributeKey(k)-> v })

  }
}
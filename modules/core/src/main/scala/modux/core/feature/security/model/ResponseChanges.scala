package modux.core.feature.security.model

import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model.headers.{HttpCookie, `Set-Cookie`}
import akka.http.scaladsl.model.{ContentType, HttpHeader, HttpResponse}
import modux.model.dsl.ResponseAsFalseFail
import modux.model.header.ResponseHeader
import org.pac4j.core.profile.UserProfile

import scala.concurrent.Future

final case class ResponseChanges(
                                  status: Int,
                                  headers: List[HttpHeader] = Nil,
                                  contentType: Option[ContentType] = None,
                                  content: Option[String] = None,
                                  cookies: List[HttpCookie] = Nil,
                                  profiles: List[UserProfile] = Nil,
                                  attributes: Map[String, AnyRef] = Map.empty) {
  def isFault: Boolean = status >= 300

  def withStatus(c: Int): ResponseChanges = ResponseChanges(c, headers, contentType, content, cookies, profiles, attributes)

  def withHeaders(c: List[HttpHeader]): ResponseChanges = ResponseChanges(status, c, contentType, content, cookies, profiles, attributes)
  def withProfiles(c: List[UserProfile]): ResponseChanges = ResponseChanges(status, headers, contentType, content, cookies, c, attributes)

  def withCookie(c: HttpCookie): ResponseChanges = ResponseChanges(status, headers, contentType, content, List(c), profiles, attributes)

  def withCookies(c: List[HttpCookie]): ResponseChanges = ResponseChanges(status, headers, contentType, content, c, profiles, attributes)
}

object ResponseChanges {

  def empty: ResponseChanges = ResponseChanges(200)

  def apply(changes: ResponseChanges, httpResponse: ResponseHeader): ResponseHeader = {

    httpResponse
      .withHeaders(changes.headers)
      .withHeaders(changes.cookies.map(x => `Set-Cookie`(x)))
//      .withProfiles(changes.profiles)
  }

  def fail[T](responseChanges: ResponseChanges)(implicit m: ToResponseMarshaller[HttpResponse]): Future[T] = Future.failed(
    ResponseAsFalseFail(
      new ToResponseMarshallable {
        override type T = HttpResponse

        override def value: HttpResponse = HttpResponse(responseChanges.status, headers = responseChanges.headers ++ responseChanges.cookies.map(x => `Set-Cookie`(x)))

        override implicit def marshaller: ToResponseMarshaller[HttpResponse] = m
      }
    )
  )

}
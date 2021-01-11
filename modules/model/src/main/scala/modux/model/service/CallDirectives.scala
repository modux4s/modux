package modux.model.service

import akka.http.scaladsl.model.{HttpMethod, HttpRequest}
import modux.model.header.{Invoke, ResponseHeader}
import org.pac4j.core.profile.UserProfile

import scala.concurrent.Future

trait CallDirectives {

  implicit protected def asDone[in, out](datum: => out): Call[in, out] = doneWith(Future.successful(datum))

  protected def mapContextAsync[IN, OUT](mapper: Invoke => Future[Invoke])(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, req) => {
    mapper(req).flatMap(x => inner(in, x))(req.executionContext)
  }

  protected def mapContext[IN, OUT](mapper: Invoke => Invoke)(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, req) => {
    inner(in, mapper(req))
  }

  protected def extractContextAsync[in, out](mapper: Invoke => Future[Call[in, out]]): Call[in, out] = (in, invoke) => {
    mapper(invoke).flatMap(x => x(in, invoke))(invoke.executionContext)
  }

  protected def extractContext[in, out](mapper: Invoke => Call[in, out]): Call[in, out] = (in, inv) => {
    mapper(inv)(in, inv)
  }

  protected def extractProfiles[in, out](mapper: Seq[UserProfile] => Call[in, out]): Call[in, out] = (in, inv) => {
    mapper(inv.profiles)(in, inv)
  }

  protected def extractRequest[in, out](mapper: HttpRequest => Call[in, out]): Call[in, out] = (in, inv) => {
    extractContext { ctx => mapper(ctx.request) }(in, inv)
  }

  protected def mapResponseAsync[IN, OUT](mapper: ResponseHeader => Future[ResponseHeader])(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, inv) => {
    mapper(inv.responseHeader).flatMap(resMod => inner(in, inv.withResponseHeader(resMod)))(inv.executionContext)
  }

  protected def mapResponse[IN, OUT](mapper: ResponseHeader => ResponseHeader)(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, req) => {
    inner(in, req.withResponseHeader(mapper(req.responseHeader)))
  }

  protected def extractMethod[in, out](handler: HttpMethod => Call[in, out]): Call[in, out] = extractRequest(req => handler(req.method))

  protected def extractBody[in, out](mapper: in => Call[in, out]): Call[in, out] = (in, req) => {
    mapper(in)(in, req)
  }

  protected def onCall[in, out](f: => Call[in, out]): Call[in, out] = (in, req) => {
    f(in, req)
  }

  protected def empty[out](f: => Call[Unit, out]): Call[Unit, out] = (_, req) => {
    f((), req)
  }

  protected def doneWith[in, out](f: => Future[out]): Call[in, out] = (_, _) => {
    f
  }
}

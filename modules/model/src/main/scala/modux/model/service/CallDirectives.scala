package modux.model.service

import akka.http.scaladsl.model.{HttpMethod, HttpRequest}
import modux.model.header.{Invoke, ResponseHeader}

import scala.concurrent.Future

trait CallDirectives {

  implicit def asDone[in, out](datum: => out): Call[in, out] = doneWith(Future.successful(datum))

  def mapContextAsync[IN, OUT](mapper: Invoke => Future[Invoke])(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, req) => {
    mapper(req).flatMap(x=> inner(in, x))(req.executionContext)
  }

  def mapContext[IN, OUT](mapper: Invoke => Invoke)(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, req) => {
    inner(in, mapper(req))
  }

  def handleContextAsync[in, out](mapper: Invoke => Future[Call[in, out]]): Call[in, out] = (in, invoke) => {
    mapper(invoke).flatMap(x => x(in, invoke))(invoke.executionContext)
  }

  def handleContext[in, out](mapper: Invoke => Call[in, out]): Call[in, out] = (in, inv) => {
    mapper(inv)(in, inv)
  }

  def handleRequest[in, out](mapper: HttpRequest => Call[in, out]): Call[in, out] = (in, inv) => {
    handleContext { ctx => mapper(ctx.request) }(in, inv)
  }

  def mapResponseAsync[IN, OUT](mapper: ResponseHeader => Future[ResponseHeader])(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, inv) => {
    mapper(inv.responseHeader).flatMap(resMod => inner(in, inv.withResponseHeader(resMod)))(inv.executionContext)
  }

  def mapResponse[IN, OUT](mapper: ResponseHeader => ResponseHeader)(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, req) => {
    inner(in, req.withResponseHeader(mapper(req.responseHeader)))
  }

  def extractMethod[in, out](handler: HttpMethod => Call[in, out]): Call[in, out] = handleRequest(req => handler(req.method))

  def extractInput[in, out](mapper: in => Call[in, out]): Call[in, out] = (in, req) => {
    mapper(in)(in, req)
  }

  def onCall[in, out](f: => Call[in, out]): Call[in, out] = (in, req) => {
    f(in, req)
  }

  def empty[out](f: => Call[Unit, out]): Call[Unit, out] = (_, req) => {
    f((), req)
  }

  def doneWith[in, out](f: => Future[out]): Call[in, out] = (_, _) => {
    f
  }

  def mapSync[IN, OUT, T](mapper: OUT => T)(inner: Call[IN, OUT]): Call[IN, T] = (in, req) => {
    inner(in, req).map(out => mapper(out))(req.executionContext)
  }

  def mapAsync[IN, OUT, T](mapper: OUT => Future[T])(inner: Call[IN, OUT]): Call[IN, T] = (in, req) => {
    import req.executionContext
    inner(in, req).flatMap(out => mapper(out))
  }
}

object CallDirectives extends CallDirectives
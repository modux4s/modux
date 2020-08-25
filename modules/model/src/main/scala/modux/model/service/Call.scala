package modux.model.service

import modux.model.header.{RequestHeader, ResponseHeader}

import scala.concurrent.{ExecutionContext, Future}

object Call {

  def apply[in, out](f: in => Future[out])(implicit ec: ExecutionContext): Call[in, out] = (x: in, _: RequestHeader, r: ResponseHeader) => f(x).map(y => (y, r))

  def empty[out](f: => Future[out])(implicit ec: ExecutionContext): Call[Unit, out] = (_: Unit, _: RequestHeader, r: ResponseHeader) => f.map(x => (x, r))

  def withRequest[in, out](f: (in, RequestHeader) => Future[out])(implicit ec: ExecutionContext): Call[in, out] = {
    (x: in, req: RequestHeader, r: ResponseHeader) => f(x, req).map(y => (y, r))
  }

  def handleRequest[in, out](f: RequestHeader => Future[out])(implicit ec: ExecutionContext): Call[in, out] = {
    (_: in, req: RequestHeader, r: ResponseHeader) => f(req).map(y => (y, r))
  }

  def compose[in, out](f: RequestHeader => Call[in, out]): Call[in, out] = {
    (x: in, req: RequestHeader, res: ResponseHeader) => f(req)(x, req, res)
  }

  def composeAsync[in, out](f: RequestHeader => Future[Call[in, out]])(implicit ec: ExecutionContext): Call[in, out] = {
    (x: in, req: RequestHeader, res: ResponseHeader) => f(req).flatMap(c => c(x, req, res))
  }
}

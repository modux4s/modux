package modux.model.service

import modux.model.header.RequestHeader

import scala.concurrent.{ExecutionContext, Future}

trait Secure {
  def resolve(f: RequestHeader): Future[RequestHeader]
}

object Secure {
  def apply[Req, Res](c: Req => Future[Res])(implicit s: Secure, ec: ExecutionContext): Call[Req, Res] =  ???

  def handleRequest[Req, Res](c: (Req, RequestHeader) => Future[Res])(implicit s: Secure, ec: ExecutionContext): Call[Req, Res] = ???
}

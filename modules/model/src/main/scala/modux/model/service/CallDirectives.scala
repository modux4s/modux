package modux.model.service

import modux.model.header.{RequestHeader, ResponseHeader}

import scala.concurrent.{Future, ExecutionContext => EC}

trait CallDirectives {

  implicit class CallExtension[IN, OUT](call: Call[IN, OUT]) {
    def mapRequest(f: RequestHeader => RequestHeader)(implicit ec: EC): Call[IN, OUT] = {
      (in: IN, x: RequestHeader, y: ResponseHeader) => call(in, f(x), y)
    }

    def mapResponse(f: ResponseHeader => ResponseHeader)(implicit ec: EC): Call[IN, OUT] = {
      (in: IN, x: RequestHeader, y: ResponseHeader) => call(in, x, f(y))
    }

    def map[T](f: OUT => T)(implicit ec: EC): Call[IN, T] = {
      (in: IN, x: RequestHeader, y: ResponseHeader) =>
        call(in, x, y).map { case (x, r) =>
          (f(x), r)
        }
    }

    def mapAsync[T](f: OUT => Future[T])(implicit ec: EC): Call[IN, T] = {
      (in: IN, x: RequestHeader, y: ResponseHeader) =>
        call(in, x, y).flatMap { case (x, r) =>
          f(x).map(y => (y, r))
        }
    }
  }

}

package modux.model.service


import modux.model.header.{RequestHeader, ResponseHeader}

import scala.concurrent.{Future, ExecutionContext => EC}
import scala.util.{Failure, Success, Try}


trait Call[IN, OUT] {

  def invoke(r: IN): Future[OUT]

  def transform(in: IN, requestHeader: RequestHeader)(implicit ec: EC): Future[(OUT, ResponseHeader)] = callFailCheck(invoke(in)).map(out => (out, ResponseHeader.Default))

  def mapRequest(f: RequestHeader => RequestHeader)(implicit ec: EC): Call[IN, OUT] = {
    val self: Call[IN, OUT] = this

    new Call[IN, OUT] {
      override def transform(in: IN, requestHeader: RequestHeader)(implicit ec: EC): Future[(OUT, ResponseHeader)] = self.transform(in, f(requestHeader))

      override def invoke(r: IN): Future[OUT] = self.transform(r, f(RequestHeader.Default)).map { case (out, _) => out }
    }
  }

  def mapResponse(f: (OUT, ResponseHeader) => ResponseHeader)(implicit ec: EC): Call[IN, OUT] = {

    val self: Call[IN, OUT] = this

    new Call[IN, OUT] {
      override def invoke(r: IN): Future[OUT] = self.invoke(r)

      override def transform(in: IN, requestHeader: RequestHeader)(implicit ec: EC): Future[(OUT, ResponseHeader)] = {
        self.transform(in, requestHeader).map { case (out, header) => out -> f(out, header) }
      }
    }
  }

  def map[T](f: (OUT, ResponseHeader) => Future[T])(implicit ec: EC): Call[IN, T] = {
    val self: Call[IN, OUT] = this

    new Call[IN, T] {
      override def invoke(r: IN): Future[T] = self.invoke(r).flatMap(out => f(out, ResponseHeader.Default))

      override def transform(in: IN, requestHeader: RequestHeader)(implicit ec: EC): Future[(T, ResponseHeader)] = {
        self.transform(in, requestHeader).flatMap { case (out, header) => f(out, header).map(_ -> header) }
      }
    }
  }

  protected def callFailCheck[T](f: => Future[T])(implicit ec: EC): Future[T] = {
    Try(f) match {
      case Failure(exception) => Future.failed(exception)
      case Success(value) => value
    }
  }
}

object Call {

  def apply[R](f: => Future[R])(implicit ec: EC): Call[Unit, R] = _ => f

  def handleRequest[IN, OUT](h: RequestHeader => Future[OUT]): Call[IN, OUT] = handleRequest((_, y) => h(y))

  def handleRequest[IN, OUT](h: (IN, RequestHeader) => Future[OUT]): Call[IN, OUT] = new Call[IN, OUT] {
    override def transform(in: IN, requestHeader: RequestHeader)(implicit ec: EC): Future[(OUT, ResponseHeader)] = h(in, requestHeader).map(out => out -> ResponseHeader.Default)

    override def invoke(r: IN): Future[OUT] = throw new RuntimeException("Must not be called")
  }

  def compose[Request, Response](f: RequestHeader => Call[Request, Response])(implicit ec: EC): Call[Request, (Response, ResponseHeader)] = {
    Call.handleRequest { (in, r) => f(r).transform(in, r) }
  }

  def composeAsync[Request, Response](f: RequestHeader => Future[Call[Request, Response]])(implicit ec: EC): Call[Request, (Response, ResponseHeader)] = {
    Call.handleRequest { (in, r) => f(r).flatMap(x => x.transform(in, r)) }
  }
}
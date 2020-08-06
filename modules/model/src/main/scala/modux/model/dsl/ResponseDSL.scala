package modux.model.dsl

import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model.StatusCode

import scala.concurrent.Future

trait ResponseDSL {

  def Ok[T](implicit m: ToResponseMarshaller[(Int, T)]): Future[T] = Custom(200)
  def Ok[T](data: T)(implicit m: ToResponseMarshaller[(Int, T)]): Future[T] = Custom(200, data)

  def Created[T](implicit m: ToResponseMarshaller[(Int, T)]): Future[T] = Custom(201)
  def Created[T](data: T)(implicit m: ToResponseMarshaller[(Int, T)]): Future[T] = Custom(201, data)

  def NotFound[T](implicit m: ToResponseMarshaller[StatusCode]): Future[T] = Custom(404)
  def NotFound[X, A](data: A)(implicit m: ToResponseMarshaller[(Int, A)]): Future[X] = Custom(404, data)

  def BadRequest[T](implicit m: ToResponseMarshaller[StatusCode]): Future[T] = Custom(400)
  def BadRequest[X, A](data: A)(implicit m: ToResponseMarshaller[(Int, A)]): Future[X] = Custom(400, data)

  def Unauthorized[T](implicit m: ToResponseMarshaller[StatusCode]): Future[T] = Custom(401)
  def Unauthorized[X, A](data: A)(implicit m: ToResponseMarshaller[(Int, A)]): Future[X] = Custom(401, data)

  def InternalError[T](implicit m: ToResponseMarshaller[StatusCode]): Future[T] = Custom(500)
  def InternalError[X, A](data: A)(implicit m: ToResponseMarshaller[(Int, A)]): Future[X] = Custom(500, data)

  def Custom[T](code: Int)(implicit m: ToResponseMarshaller[StatusCode]): Future[T] = Future.failed(ResponseAsFalseFail(asStatusCode(code)))
  def Custom[T, A](code: Int, data: A)(implicit m: ToResponseMarshaller[(Int, A)]): Future[T] = Future.failed(ResponseAsFalseFail(asEntity(code, data)))

  protected implicit def asCustom[T, A](x: (Int, A))(implicit m: ToResponseMarshaller[(Int, A)]): Future[T] = Custom(x._1, x._2)

  private def asStatusCode(code: Int)(implicit m: ToResponseMarshaller[StatusCode]): ToResponseMarshallable = new ToResponseMarshallable {
    override type T = StatusCode

    override def value: StatusCode = StatusCode.int2StatusCode(code)

    override implicit def marshaller: ToResponseMarshaller[StatusCode] = m
  }

  private def asEntity[A](code: Int, data: A)(implicit m: ToResponseMarshaller[(Int, A)]): ToResponseMarshallable = new ToResponseMarshallable {
    override type T = (Int, A)

    override def value: (Int, A) = (code, data)

    override implicit def marshaller: ToResponseMarshaller[(Int, A)] = m
  }
}

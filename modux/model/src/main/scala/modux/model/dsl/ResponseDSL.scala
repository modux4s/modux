package modux.model.dsl

import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model.StatusCode

import scala.concurrent.Future

trait ResponseDSL {

  def NotFound[T](implicit m: ToResponseMarshaller[StatusCode]): Future[T] = Custom(404)
  def NotFound[X, A](data: A)(implicit m: ToResponseMarshaller[(Int, A)]): Future[X] = Custom(404, data)

  def Unauthorized[T](implicit m: ToResponseMarshaller[StatusCode]): Future[T] = Custom(401)
  def Unauthorized[X, A](data: A)(implicit m: ToResponseMarshaller[(Int, A)]): Future[X] = Custom(401, data)

  def InternalError[T](implicit m: ToResponseMarshaller[StatusCode]): Future[T] = Custom(500)
  def InternalError[X, A](data: A)(implicit m: ToResponseMarshaller[(Int, A)]): Future[X] = Custom(500, data)

  def Custom[T](code: Int)(implicit m: ToResponseMarshaller[StatusCode]): Future[T] = Future.failed(ModuxResponseException(asStatusCode(code)))
  def Custom[T, A](code: Int, data: A)(implicit m: ToResponseMarshaller[(Int, A)]): Future[T] = Future.failed(ModuxResponseException(asEntity(code, data)))

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

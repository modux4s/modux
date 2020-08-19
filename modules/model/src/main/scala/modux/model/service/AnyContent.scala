package modux.model.service

import akka.NotUsed
import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model._
import modux.model.content.{Content, WithContent, WithoutContent}

import scala.concurrent.{ExecutionContext, Future}

trait AnyContent extends ToResponseMarshallable

object AnyContent {

  private def mapper[A, IN](c: Call[IN, Content])(implicit ec: ExecutionContext, strMarshaller: ToResponseMarshaller[String], scMarshaller: ToResponseMarshaller[StatusCode]): Call[IN, AnyContent] = {
    c
      .mapResponse { case (content, header) =>
        header
          .withStatus(content.status)
          .withHeaders(content.headers)
          .withCookies(content.cookies)
      }
      .map { (content, _) =>

        Future {
          content match {
            case x: WithContent[A@unchecked] =>

              new AnyContent {
                override type T = A

                override def value: A = x.body

                override implicit def marshaller: ToResponseMarshaller[A] = x.marshaller
              }

            case x: WithoutContent =>

              x.message match {
                case Some(y) =>

                  new AnyContent {
                    override type T = String

                    override def value: String = y

                    override implicit def marshaller: ToResponseMarshaller[String] = strMarshaller
                  }
                case None =>

                  new AnyContent {
                    override type T = StatusCode

                    override def value: StatusCode = x.status

                    override implicit def marshaller: ToResponseMarshaller[StatusCode] = scMarshaller
                  }
              }
          }
        }
      }
  }

  def apply[T](f: => Future[Content])(implicit ec: ExecutionContext): Call[NotUsed, AnyContent] = mapper(Call.handleRequest((_, _) => f))
}

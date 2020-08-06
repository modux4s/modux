package modux.macros.serializer.codec.providers.api

import akka.http.scaladsl.marshalling.{Marshaller, Marshalling, ToByteStringMarshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentType, HttpCharsets}
import akka.http.scaladsl.unmarshalling.{FromByteStringUnmarshaller, FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

trait CodecEntityProvider extends CodecProvider {

  def fromByteString[A](bs: ByteString)(implicit mf: Manifest[A], ec: ExecutionContext): Future[A]
  def toByteString[A](data: A)(implicit mf: Manifest[A], ec: ExecutionContext): Future[ByteString]

  def marshaller[A](implicit mf: Manifest[A]): ToEntityMarshaller[A]
  def unmarshaller[A](implicit mf: Manifest[A]): FromEntityUnmarshaller[A]

  def fromByteStringUnmarshaller[A](implicit mf: Manifest[A]): FromByteStringUnmarshaller[A] = {

    Unmarshaller.withMaterializer[ByteString, A] { implicit ec =>
      _ =>
        bs =>
          fromByteString(bs)
    }
  }

  def toByteStringMarshaller[T](implicit mf: Manifest[T]): ToByteStringMarshaller[T] = {
    Marshaller[T, ByteString] { implicit ec =>
      data =>
        toByteString(data).map { x =>
          List(
            Marshalling.WithFixedContentType(ContentType(mediaType, () => HttpCharsets.`UTF-8`), () => x)
          )
        }
    }
  }
}

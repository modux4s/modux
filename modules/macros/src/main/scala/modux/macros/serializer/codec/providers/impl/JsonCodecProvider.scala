package modux.macros.serializer.codec.providers.impl

import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import akka.util.ByteString
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter}
import modux.macros.serializer.codec.providers.api.CodecMixedProvider

import scala.concurrent.{ExecutionContext, Future}


final case class JsonCodecProvider(maxObjSize: Int = 8 * 1024) extends CodecMixedProvider {

  private lazy val mt: MediaType.WithFixedCharset = MediaTypes.`application/json`
  private lazy val contentTypeRange: ContentTypeRange = ContentTypeRange(mt)
  private lazy val jsonMapper: ObjectMapper = CodecUtils.createJsonMapper()
  private lazy val objectWriter: ObjectWriter = jsonMapper.writerWithDefaultPrettyPrinter()

  override def streaming: EntityStreamingSupport = EntityStreamingSupport.json(maxObjSize)

  override def mediaType: MediaType = mt

  override def marshaller[A](implicit mf: Manifest[A]): ToEntityMarshaller[A] = {

    Marshaller
      .withFixedContentType(mt) { x =>
        HttpEntity(ContentType(mt), ByteString(jsonMapper.writeValueAsString(x)))
      }
  }

  override def unmarshaller[A](implicit mf: Manifest[A]): FromEntityUnmarshaller[A] = {
    Unmarshaller
      .byteStringUnmarshaller
      .forContentTypes(contentTypeRange)
      .map { data =>
        jsonMapper.readValue(data.utf8String, mf.runtimeClass).asInstanceOf[A]
      }
  }

  override def fromByteString[A](bs: ByteString)(implicit mf: Manifest[A], ec: ExecutionContext): Future[A] = FastFuture.successful {
    jsonMapper.readValue(bs.utf8String, mf.runtimeClass).asInstanceOf[A]
  }

  override def toByteString[A](data: A)(implicit mf: Manifest[A], ec: ExecutionContext): Future[ByteString] = FastFuture.successful {
    ByteString(objectWriter.writeValueAsString(data))
  }
}

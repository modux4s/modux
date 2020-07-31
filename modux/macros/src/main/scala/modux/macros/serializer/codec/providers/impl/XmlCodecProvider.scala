package modux.macros.serializer.codec.providers.impl

import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter}
import modux.macros.serializer.codec.providers.api.CodecMixedProvider
import modux.macros.serializer.streaming.XmlStreamSupport

import scala.concurrent.{ExecutionContext, Future}


final case class XmlCodecProvider(maxObjSize: Int = 8 * 1024) extends CodecMixedProvider {

  private lazy val mt: MediaType.WithOpenCharset = MediaTypes.`application/xml`
  private lazy val ct: ContentType = ContentType(mt, () => HttpCharsets.`UTF-8`)
  private lazy val contentTypeRange: ContentTypeRange = ContentTypeRange(mt)
  private lazy val stream: XmlStreamSupport = new XmlStreamSupport(maxObjSize)

  private lazy val xmlMapper: ObjectMapper = CodecUtils.createXmlMapper()
  private lazy val prettyPrinter: ObjectWriter = xmlMapper.writerWithDefaultPrettyPrinter

  override def mediaType: MediaType = mt

  override def streaming: EntityStreamingSupport = stream

  override def marshaller[A](implicit mf: Manifest[A]): ToEntityMarshaller[A] = {
    Marshaller.withFixedContentType(ct)(x => HttpEntity(ct, ByteString(xmlMapper.writeValueAsString(x))))
  }

  override def unmarshaller[A](implicit mf: Manifest[A]): FromEntityUnmarshaller[A] = {
    Unmarshaller
      .byteStringUnmarshaller
      .forContentTypes(contentTypeRange)
      .map(data => xmlMapper.readValue(data.utf8String, mf.runtimeClass).asInstanceOf[A])
  }

  override def fromByteString[A](bs: ByteString)(implicit mf: Manifest[A], ec: ExecutionContext): Future[A] = Future {
    xmlMapper.readValue(bs.utf8String, mf.runtimeClass).asInstanceOf[A]
  }

  override def toByteString[A](data: A)(implicit mf: Manifest[A], ec: ExecutionContext): Future[ByteString] = Future {
    ByteString(prettyPrinter.writeValueAsString(data))
  }
}

package modux.macros.serializer.streaming

import akka.NotUsed
import akka.http.javadsl.model
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.model.{ContentType, ContentTypeRange, HttpCharsets, MediaTypes}
import akka.stream.scaladsl.Flow
import akka.util.ByteString

class XmlStreamSupport(
                        val maximumTextLength: Int,
                        val supported: ContentTypeRange,
                        val contentType: ContentType,
                        val parallelism: Int = 1,
                        val unordered: Boolean = true
                      ) extends EntityStreamingSupport {

  def this(maxLg: Int) = {
    this(maxLg, ContentTypeRange(MediaTypes.`application/xml`), ContentType(MediaTypes.`application/xml`, HttpCharsets.`UTF-8`))
  }

  override def framingDecoder: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].via(new StreamingXmlParser())

  override def framingRenderer: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString].intersperse(ByteString("\n"))

  override def withSupported(range: model.ContentTypeRange): XmlStreamSupport = this

  override def withContentType(range: model.ContentType): XmlStreamSupport = this

  override def withParallelMarshalling(parallelism: Int, unordered: Boolean): EntityStreamingSupport = this
}

package modux.macros.serializer

import akka.NotUsed
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.marshalling._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshaller.UnsupportedContentTypeException
import akka.http.scaladsl.unmarshalling.{FromByteStringUnmarshaller, FromEntityUnmarshaller, FromRequestUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import akka.stream.scaladsl.{Flow, Keep, Source}
import akka.util.ByteString
import modux.macros.serializer.codec._
import modux.macros.serializer.codec.providers.api.{CodecEntityProvider, CodecMixedProvider, CodecProvider}
import modux.macros.serializer.codec.providers.impl.{CsvStreamProvider, JsonCodecProvider, XmlCodecProvider}
import modux.macros.serializer.websocket.JsonWebSocketCodecImpl
import modux.model.converter.WebSocketCodec

trait SerializationSupport {

  private type TRM[T] = ToResponseMarshaller[T]

  protected def codecFor[A, B]: WebSocketCodec[A, B] = macro JsonWebSocketCodecImpl.websocket[A, B]

  protected def codecFor[T](implicit codecRegistry: CodecRegistry = SerializationSupport.DefaultCodecRegistry, mf: Manifest[T]): Codec[T] = new Codec[T] {
    override def marshaller(implicit mf: Manifest[T]): ToEntityMarshaller[T] = asEntityMarshaller[T]

    override def unmarshaller(implicit mf: Manifest[T]): FromEntityUnmarshaller[T] = unmarshallerBuilder[T]
  }

  protected implicit def asToByteStringMarshaller[T](implicit mf: Manifest[T], codec: CodecRegistry = SerializationSupport.DefaultCodecRegistry): ToByteStringMarshaller[T] = {
    Marshaller.oneOf(codec.entityProvider: _*)(_.toByteStringMarshaller[T])
  }

  protected implicit def toCodecRegistry(c: CodecProvider): CodecRegistry = codecRegistry(c)

  protected implicit def asUnmarshaller[A](implicit mf: Manifest[A], codec: Codec[A]): FromEntityUnmarshaller[A] = {
    codec.unmarshaller
  }

  protected implicit def moduxRes[T, M](implicit codec: Codec[T], mf: Manifest[T], codecRegistry: CodecRegistry = SerializationSupport.DefaultCodecRegistry): Marshaller[Source[T, M], HttpResponse] = {

    val marshaller: Marshaller[T, Source[ByteString, _]] = codec.marshaller.map(_.dataBytes)

    Marshaller.oneOf[EntityStreamingSupport, Source[T, M], HttpResponse](codecRegistry.streams(): _*) { stream =>
      Marshaller[Source[T, M], HttpResponse] { implicit ec =>
        src =>
          FastFuture.successful {
            Marshalling.WithFixedContentType(stream.contentType, () => {

              val bestMarshallingPerElement: Source[() => Source[ByteString, _], M] = src
                .mapAsync(stream.parallelism)(x => marshaller(x))
                .map { xs =>
                  selectMarshallingForContentType(xs, stream.contentType)
                    .orElse {
                      xs collectFirst { case Marshalling.Opaque(marshal) => marshal }
                    }
                    .getOrElse(throw new RuntimeException(stream.contentType.toString()))
                }

              val marshalledElements: Source[ByteString, M] =
                bestMarshallingPerElement
                  .flatMapConcat(_.apply()) // marshal!
                  .via(stream.framingRenderer)

              HttpResponse(entity = HttpEntity(stream.contentType, marshalledElements))
            }) :: Nil
          }
      }
    }
  }

  protected implicit def asEntityMarshallerFromCodec[A](implicit mf: Manifest[A], codec: Codec[A]): ToEntityMarshaller[A] = codec.marshaller

  private def unmarshallerBuilder[A](implicit mf: Manifest[A], cr: CodecRegistry): FromEntityUnmarshaller[A] = {

    Unmarshaller.withMaterializer[HttpEntity, A] { implicit ec =>
      implicit mat =>
        entity =>

          val entityMediaType: MediaType = entity.contentType.mediaType
          val datum: Option[CodecEntityProvider] = cr
            .codecs
            .flatMap(_.codecs)
            .collect { case x: CodecEntityProvider => x }
            .find(x => x.mediaType.matches(entityMediaType))

          datum match {
            case Some(codecProvider) =>
              codecProvider.unmarshaller[A].apply(entity)
            case None => throw new RuntimeException("no unmarshall founded")
          }
    }
  }

  private def asEntityMarshaller[A](implicit mf: Manifest[A], cr: CodecRegistry): ToEntityMarshaller[A] = {
    Marshaller.oneOf(cr.codecs.collect { case x: CodecEntityProvider => x }.map(_.marshaller[A]): _*)
  }

  private def codecRegistry(c: CodecProvider*): CodecRegistry = new CodecRegistry {
    override def codecs: Seq[CodecProvider] = c
  }

  private def selectMarshallingForContentType[T](marshallings: Seq[Marshalling[T]], contentType: ContentType): Option[() => T] = {
    contentType match {
      case _: ContentType.Binary | _: ContentType.WithFixedCharset | _: ContentType.WithMissingCharset =>
        marshallings collectFirst { case Marshalling.WithFixedContentType(`contentType`, marshal) => marshal }
      case ContentType.WithCharset(mediaType, charset) =>
        marshallings collectFirst {
          case Marshalling.WithFixedContentType(`contentType`, marshal) => marshal
          case Marshalling.WithOpenCharset(`mediaType`, marshal) => () => marshal(charset)
        }
    }
  }
}

object SerializationSupport {

  implicit final val DefaultCodecRegistry: CodecRegistry = {
    new CodecRegistry {
      override def codecs: Seq[CodecProvider] = Seq(JsonCodecProvider(), XmlCodecProvider(), CsvStreamProvider())
    }
  }

  private type RequestToSourceUnmarshaller[T] = FromRequestUnmarshaller[Source[T, NotUsed]]

  private[modux] def moduxAsSource[T](implicit mf: Manifest[T], cr: CodecRegistry): RequestToSourceUnmarshaller[T] =
    Unmarshaller.withMaterializer[HttpRequest, Source[T, NotUsed]] { implicit ec =>
      implicit mat =>
        req =>

          val entity: RequestEntity = req.entity
          val contentType: ContentType = entity.contentType

          cr
            .codecs
            .collect {
              case provider: CodecMixedProvider => provider
            }
            .find(x => x.streaming.supported.matches(contentType)) match {
            case Some(codec) =>

              val support: EntityStreamingSupport = codec.streaming
              val um: FromByteStringUnmarshaller[T] = codec.fromByteStringUnmarshaller[T]
              val bytes: Source[ByteString, Any] = entity.dataBytes
              val frames: Source[ByteString, Any] = bytes.via(support.framingDecoder)
              val marshalling: Flow[ByteString, T, NotUsed] = {
                if (support.unordered)
                  Flow[ByteString].mapAsyncUnordered(support.parallelism)(bs => um(bs)(ec, mat))
                else
                  Flow[ByteString].mapAsync(support.parallelism)(bs => um(bs)(ec, mat))
              }

              val elements: Source[T, NotUsed] = frames.viaMat(marshalling)(Keep.right)
              FastFuture.successful(elements)

            case None => FastFuture.failed(UnsupportedContentTypeException(Option(contentType)))
          }
    }
}



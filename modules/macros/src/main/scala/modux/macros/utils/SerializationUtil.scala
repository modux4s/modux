package modux.macros.utils

import akka.NotUsed
import akka.http.scaladsl.common.EntityStreamingSupport
import akka.http.scaladsl.model.{ContentType, HttpRequest, RequestEntity}
import akka.http.scaladsl.unmarshalling.{FromByteStringUnmarshaller, FromRequestUnmarshaller, Unmarshaller}
import akka.http.scaladsl.unmarshalling.Unmarshaller.UnsupportedContentTypeException
import akka.http.scaladsl.util.FastFuture
import akka.stream.scaladsl.{Flow, Keep, Source}
import akka.util.ByteString
import modux.macros.serializer.codec.CodecRegistry
import modux.macros.serializer.codec.providers.api.CodecMixedProvider

object SerializationUtil {

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

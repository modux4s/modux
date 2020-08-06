package modux.macros.serializer.codec

import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller

trait Codec[T] {
  def marshaller(implicit mf: Manifest[T]): ToEntityMarshaller[T]
  def unmarshaller(implicit mf: Manifest[T]): FromEntityUnmarshaller[T]
}

package modux.macros.serializer.codec

import akka.http.scaladsl.common.EntityStreamingSupport
import modux.macros.serializer.codec.providers.api.{CodecProvider, CodecStreamProvider}

trait CodecRegistry {
  private lazy val streaming: Seq[EntityStreamingSupport] = codecs.collect { case x: CodecStreamProvider => x }.map(_.streaming)
  def codecs: Seq[CodecProvider]
  def streams(): Seq[EntityStreamingSupport] = streaming
}

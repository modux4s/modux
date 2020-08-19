package modux.macros.serializer.codec

import akka.http.scaladsl.common.EntityStreamingSupport
import modux.macros.serializer.codec.providers.api.{CodecEntityProvider, CodecProvider, CodecStreamProvider}

trait CodecRegistry {
  private lazy val streaming: Seq[EntityStreamingSupport] = codecs.collect { case x: CodecStreamProvider => x }.map(_.streaming)
  lazy val entityProvider: Seq[CodecEntityProvider] = codecs.collect { case x: CodecEntityProvider => x }

  def codecs: Seq[CodecProvider]
  def streams(): Seq[EntityStreamingSupport] = streaming

}

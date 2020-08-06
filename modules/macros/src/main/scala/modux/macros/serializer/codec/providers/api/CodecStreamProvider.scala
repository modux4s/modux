package modux.macros.serializer.codec.providers.api

import akka.http.scaladsl.common.EntityStreamingSupport

trait CodecStreamProvider  extends CodecProvider {
  def streaming: EntityStreamingSupport
}

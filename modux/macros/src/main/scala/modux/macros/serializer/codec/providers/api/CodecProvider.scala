package modux.macros.serializer.codec.providers.api

import akka.http.scaladsl.model.MediaType

trait CodecProvider {
  def mediaType: MediaType
}




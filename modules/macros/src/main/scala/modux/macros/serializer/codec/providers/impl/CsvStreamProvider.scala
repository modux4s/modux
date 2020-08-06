package modux.macros.serializer.codec.providers.impl

import akka.http.scaladsl.common.{CsvEntityStreamingSupport, EntityStreamingSupport}
import akka.http.scaladsl.model.{MediaType, MediaTypes}
import modux.macros.serializer.codec.providers.api.CodecStreamProvider

final case class CsvStreamProvider(maxObjSize: Int = 8 * 1024) extends CodecStreamProvider {
  override def streaming: CsvEntityStreamingSupport = EntityStreamingSupport.csv(maxObjSize)

  override def mediaType: MediaType = MediaTypes.`text/csv`
}

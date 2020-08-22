package modux.macros.serializer

import modux.macros.serializer.codec.CodecRegistry
import modux.macros.serializer.codec.providers.api.CodecProvider
import modux.macros.serializer.codec.providers.impl.{CsvStreamProvider, JsonCodecProvider, XmlCodecProvider}

object SerializationDefaults {
  final val DefaultCodecRegistry: CodecRegistry = {
    new CodecRegistry {
      override def codecs: Seq[CodecProvider] = Seq(JsonCodecProvider(), XmlCodecProvider(), CsvStreamProvider())
    }
  }
}

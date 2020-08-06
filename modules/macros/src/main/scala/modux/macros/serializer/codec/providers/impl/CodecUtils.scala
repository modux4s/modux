package modux.macros.serializer.codec.providers.impl

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object CodecUtils {
  def createJsonMapper(): ObjectMapper = {
    new ObjectMapper()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .setSerializationInclusion(Include.NON_NULL)
      .registerModule(DefaultScalaModule)
      .registerModule(new JavaTimeModule())
  }

  def createXmlMapper(): ObjectMapper = {
    new XmlMapper()
      .registerModule(DefaultScalaModule)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .setSerializationInclusion(Include.NON_NULL)
      .registerModule(new JavaTimeModule())
  }

  def createYamlMapper(): ObjectMapper = {
    new YAMLMapper()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .setSerializationInclusion(Include.NON_NULL)
      .registerModule(new JavaTimeModule())
  }
}

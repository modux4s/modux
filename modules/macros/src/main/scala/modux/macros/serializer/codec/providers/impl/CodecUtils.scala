package modux.macros.serializer.codec.providers.impl

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import com.fasterxml.jackson.dataformat.xml.{JacksonXmlModule, XmlFactory, XmlMapper}
import com.fasterxml.jackson.dataformat.yaml.{YAMLFactory, YAMLMapper}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule

object CodecUtils {

  private final val json: ObjectMapper = prepare(new ObjectMapper())
  private final val xml: ObjectMapper = prepare {
    /*val module: JacksonXmlModule = new JacksonXmlModule()
    module.setDefaultUseWrapper(false)
    new XmlMapper(module)*/
//    new ObjectMapper(new XmlFactory().configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, false))
    new XmlMapper()//.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, false)
  }
  private final val yaml: ObjectMapper = prepare(new ObjectMapper(new YAMLFactory()))

  private def prepare(d: ObjectMapper): ObjectMapper = {
    d
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .setSerializationInclusion(Include.NON_NULL)
      .registerModule(DefaultScalaModule)
      .registerModule(new JavaTimeModule())
  }

  def createJsonMapper(): ObjectMapper = json

  def createXmlMapper(): ObjectMapper = xml

  def createYamlMapper(): ObjectMapper = yaml
}

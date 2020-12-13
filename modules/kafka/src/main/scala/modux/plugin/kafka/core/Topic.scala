package modux.plugin.kafka.core

import akka.http.scaladsl.util.FastFuture
import com.fasterxml.jackson.databind.ObjectMapper
import modux.macros.serializer.codec.providers.impl.CodecUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.record.TimestampType

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Try

case class Topic private(consumerRecord: ConsumerRecord[String, String]) {
  def topic: String = consumerRecord.topic()

  def partition: Int = consumerRecord.partition()

  def timestamp: Long = consumerRecord.timestamp()

  def timestampType: TimestampType = consumerRecord.timestampType()

  def asString: String = consumerRecord.value()

  def fromJson[A](implicit classTag: ClassTag[A]): Future[A] = converter(CodecUtils.createJsonMapper())

  def fromYaml[A](implicit classTag: ClassTag[A]): Future[A] = converter(CodecUtils.createYamlMapper())

  def fromXml[A](implicit classTag: ClassTag[A]): Future[A] = converter(CodecUtils.createXmlMapper())

  final private def converter[A](mapper: ObjectMapper)(implicit classTag: ClassTag[A]): Future[A] = FastFuture {
    Try(mapper.readValue(consumerRecord.value(), classTag.runtimeClass).asInstanceOf[A])
  }
}

package modux.plugin.kafka.core

import java.util.Properties
import scala.collection.mutable

final case class KafkaConfig() {
  private val store: mutable.Map[String, String] = mutable.Map.empty

  def asProperty(): Properties = {
    val props = new Properties
    store.foreach { case (str, value) => props.setProperty(str, value) }
    props
  }

  def withProperty(name: String, value: String): KafkaConfig = {
    store.put(name, value)
    this
  }
}
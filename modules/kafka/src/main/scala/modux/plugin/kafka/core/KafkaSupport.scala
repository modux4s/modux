package modux.plugin.kafka.core

import modux.model.{ServiceDef, ServiceEntry}
import modux.model.context.Context

trait KafkaSupport {

  def topic(topicName: String, call: Topic => Unit)(implicit context: Context): ServiceEntry = {
    KafkaSupportService(topicName, call, context)
  }

}

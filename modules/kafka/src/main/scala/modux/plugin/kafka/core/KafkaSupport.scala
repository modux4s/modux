package modux.plugin.kafka.core

import modux.model.ServiceDef
import modux.model.context.Context
import org.apache.kafka.clients.consumer.ConsumerRecord

trait KafkaSupport {

  protected implicit class KafkaSupportUtils(service: ServiceDef) {
    def topic(topicName: String, call: ConsumerRecord[String, String] => Unit)(implicit context: Context): ServiceDef = {
      service.copy(
        servicesCall = service.servicesCall :+ KafkaSupportService(topicName, call, context)
      )
    }
  }

}

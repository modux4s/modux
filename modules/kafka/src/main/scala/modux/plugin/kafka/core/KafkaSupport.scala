package modux.plugin.kafka.core

import modux.model.ServiceDef
import modux.model.context.Context

trait KafkaSupport {

  protected implicit class KafkaSupportUtils(service: ServiceDef) {
    def topic(topicName: String, call: Topic => Unit)(implicit context: Context): ServiceDef = {
      service.copy(servicesCall = service.servicesCall :+ KafkaSupportService(topicName, call, context))
    }
  }

}

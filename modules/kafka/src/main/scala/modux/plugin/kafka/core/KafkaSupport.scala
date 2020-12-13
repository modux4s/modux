package modux.plugin.kafka.core

import modux.model.ServiceEntry
import modux.model.context.Context

import scala.concurrent.Future

trait KafkaSupport {

  def topic(topicName: String, call: Topic => Future[Unit])(implicit context: Context): ServiceEntry = {
    KafkaSupportService(topicName, call, context)
  }

}

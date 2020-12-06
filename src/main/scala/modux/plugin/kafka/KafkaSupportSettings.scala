package modux.plugin.kafka

import sbt.{settingKey, taskKey}

trait KafkaSupportSettings {

  val kafkaVersion = settingKey[String]("Kafka version")

}

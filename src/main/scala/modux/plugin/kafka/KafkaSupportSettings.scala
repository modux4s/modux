package modux.plugin.kafka

import sbt.{settingKey, taskKey}

trait KafkaSupportSettings {

  val kafkaVersion = settingKey[String]("Kafka version")
  val startKafka = taskKey[Unit]("Download and start kafka")
  val stopKafka = taskKey[Unit]("Stop kafka")

}

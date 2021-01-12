package modux.plugin.kafka

import sbt.{SettingKey, settingKey}

trait KafkaSupportSettings {

  val kafkaVersion: SettingKey[String] = settingKey[String]("Kafka version")
  val autoLoadKafka: SettingKey[Boolean] = settingKey[Boolean]("Auto download and start kafka server")

}

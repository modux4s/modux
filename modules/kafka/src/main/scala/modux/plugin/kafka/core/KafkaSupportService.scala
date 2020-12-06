package modux.plugin.kafka.core

import com.typesafe.scalalogging.LazyLogging
import modux.model.ServiceEntry
import modux.model.context.Context
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}

import java.time.Duration
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Try}

case class KafkaSupportService(topicName: String, call: Topic => Unit, context: Context) extends ServiceEntry with LazyLogging with Runnable {

  private final val keepRunning: AtomicBoolean = new AtomicBoolean(true)
  private final val properties: Properties = new Properties()
  properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  properties.put(ConsumerConfig.GROUP_ID_CONFIG, context.applicationName)
  properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

  private lazy val consumer: KafkaConsumer[String, String] = new KafkaConsumer[String, String](properties)

  override def run(): Unit = {
    consumer.subscribe(Seq(topicName).asJava)
    logger.info("starting consumer")

    while (keepRunning.get()) {
      consumer.poll(Duration.ofSeconds(10)).forEach { record =>
        Try(call(Topic(record))) match {
          case Failure(exception) => logger.error(exception.getLocalizedMessage, exception)
          case _ =>
        }
      }
      Try(consumer.commitAsync()).recover { case t => logger.error(t.getLocalizedMessage, t) }

    }
    Try(consumer.close(Duration.ofSeconds(5))).recover { case t => logger.error(t.getLocalizedMessage, t) }
  }

  override def onStart(): Unit = {
    new Thread(this).start()
  }

  override def onStop(): Unit = {
    keepRunning.set(false)
  }
}

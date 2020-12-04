package modux.plugin.kafka.core

import com.typesafe.scalalogging.LazyLogging
import modux.model.ServiceEntry
import modux.model.context.Context
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, KafkaConsumer}

import java.time.Duration
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Try}

case class KafkaSupportService[A](topicName: String, call: ConsumerRecord[String, String] => Unit, context: Context) extends ServiceEntry with LazyLogging {
  private val run = new AtomicBoolean(true)

  private val properties = new Properties()
  properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  properties.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer-tutorial")
  properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

  private lazy val thread = {
    val consumer: KafkaConsumer[String, String] = new KafkaConsumer[String, String](properties)
    consumer.subscribe(Seq(topicName).asJava)

    new Thread(() => {

      while (run.get()) {
        consumer.poll(Duration.ofSeconds(10)).forEach { record =>
          Try(call(record)) match {
            case Failure(exception) => logger.error(exception.getLocalizedMessage, exception)
            case _ =>
          }
        }

      }

      Try(consumer.close(Duration.ofSeconds(5)))
    })
  }

  override def onStart(): Unit = {
    thread.start()
  }

  override def onStop(): Unit = {
    run.set(false)
  }
}

package modux.plugin.kafka.core

import akka.Done
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import com.typesafe.scalalogging.LazyLogging
import modux.model.ServiceEntry
import modux.model.context.Context
import org.apache.kafka.common.serialization.StringDeserializer

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure
import com.github.andyglow.config._
import org.apache.kafka.clients.consumer.ConsumerConfig

import java.util.UUID

case class KafkaSupportService(topicName: String, call: Topic => Future[Unit], context: Context) extends ServiceEntry with LazyLogging /*with Runnable*/ {

  private implicit val sys: ActorSystem[Nothing] = context.actorSystem
  private implicit val ec: ExecutionContext = context.executionContext
  private val committerSettings: CommitterSettings = CommitterSettings(context.actorSystem)
  private val consumerSettings: ConsumerSettings[String, String] = {
    ConsumerSettings(sys, new StringDeserializer, new StringDeserializer)
      .withBootstrapServers(context.config.get[Option[String]]("akka.kafka.bootstrap").getOrElse("localhost:9092"))
      .withProperty(ConsumerConfig.GROUP_ID_CONFIG, context.applicationName)
      .withProperty(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString)
  }
  private val reference: AtomicReference[DrainingControl[Done]] = new AtomicReference[DrainingControl[Done]]()

  //  private final val keepRunning: AtomicBoolean = new AtomicBoolean(true)
  //  private final val properties: Properties = new Properties()
  //  properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  //  properties.put(ConsumerConfig.GROUP_ID_CONFIG, context.applicationName)
  //  properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
  //  properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
  //  properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
  //  properties.put(ConsumerConfig.CLIENT_ID_CONFIG, UUID.randomUUID().toString)
  //  properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")

  //  private lazy val consumer: KafkaConsumer[String, String] = new KafkaConsumer[String, String](properties)

  /*
    override def run(): Unit = {
      consumer.subscribe(Seq(topicName).asJava)

      Try {
        while (keepRunning.get()) {
          val records: ConsumerRecords[String, String] = consumer.poll(Duration.ofSeconds(10))

          records.forEach { record =>
            Try(call(Topic(record))) match {
              case Failure(exception) => logger.error(exception.getLocalizedMessage, exception)
              case _ =>
            }
          }
          Try {
            if (!records.isEmpty) {
              consumer.commitSync()
            }
          }.recover { case t => logger.error(t.getLocalizedMessage, t) }
        }
      }.recover { case t => t.printStackTrace() }

      consumer.unsubscribe()
      Try(consumer.close()).recover { case t => logger.error(t.getLocalizedMessage, t) }
    }
  */

  override def onStart(): Unit = {
    //    new Thread(this).start()
    reference.set {
      Consumer
        .committableSource(consumerSettings, Subscriptions.topics(topicName))
        .mapAsync(1)(msg => call(Topic(msg.record)).map(_ => msg.committableOffset))
        .toMat(Committer.sink(committerSettings))(DrainingControl.apply)
        .run()
    }
  }

  override def onStop(): Unit = {
    Option(reference.get()).foreach { control =>
      control.drainAndShutdown().onComplete {
        case Failure(exception) => logger.error(exception.getLocalizedMessage, exception)
        case _ =>
      }
    }
  }
}

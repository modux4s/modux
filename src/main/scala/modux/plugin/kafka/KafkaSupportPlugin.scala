package modux.plugin.kafka

import modux.plug.HookPlugin
import modux.plugin.ModuxPluginDefaults.moduxKafka
import modux.plugin.classutils.FileTool
import modux.plugin.kafka.service.{KafkaService, ZookeeperService}
import sbt.Keys._
import sbt.internal.util.ManagedLogger
import sbt.io.IO
import sbt.plugins.JvmPlugin
import sbt.{AutoPlugin, Def, Plugins, url, _}

import java.util.concurrent.atomic.AtomicReference
import scala.reflect.io.Directory
import scala.sys.process._
import scala.util.Try

object KafkaSupportPlugin extends AutoPlugin {

  override def trigger = noTrigger

  override def requires: Plugins = JvmPlugin && HookPlugin

  object autoImport extends KafkaSupportSettings {
  }

  import autoImport._
  import modux.plug.HookPlugin.autoImport._

  private val isLinux: Boolean = System.getProperty("os.name").toLowerCase match {
    case win if win.contains("win") => false
    case other if other.contains("mac") || other.contains("linux") => true
    case osName => throw new RuntimeException(s"Unknown operating system $osName")
  }

  private lazy val zookeeperPID = new AtomicReference[ZookeeperService]()
  private lazy val kafkaPID = new AtomicReference[KafkaService]()

  private val startTask: Def.Initialize[Task[Unit]] = Def.task[Unit] {
    val logger: ManagedLogger = streams.value.log
    val kafkaV: String = kafkaVersion.value
    val baseDir: File = target.value

    if (autoLoadKafka.value) {

      {
        for {
          _ <- Try(doInit(baseDir / "kafka"))
          _ <- Try(doStop(logger))
          _ <- Try(doDownload(kafkaV, baseDir, logger))
          _ <- Try(doStart(logger))
        } yield ()
      }.recover { case t => t.printStackTrace() }
    }
  }
  private val stopTask: Def.Initialize[Task[Unit]] = Def.task[Unit] {
    val logger: ManagedLogger = streams.value.log
    if (autoLoadKafka.value) {
      doStop(logger)
      logger.info("Kafka stopped")
    }
  }

  override lazy val projectSettings: Seq[Setting[_]] = Seq(

    kafkaVersion := "2.6.0",
    autoLoadKafka := true,
    libraryDependencies ++= Seq(moduxKafka),
    resolvers += "io.confluent" at "https://packages.confluent.io/maven/",
    startHook += startTask,
    stopHook += stopTask
  )

  private def doInit(kafkaHome: File): Unit = {
    Option(zookeeperPID.get()) match {
      case None => zookeeperPID.set(ZookeeperService(isLinux, kafkaHome))
      case _ =>
    }

    Option(kafkaPID.get()) match {
      case None => kafkaPID.set(KafkaService(isLinux, kafkaHome))
      case _ =>
    }
  }

  private def doDownload(kafkaVersion: String, baseDir: File, logger: ManagedLogger): Unit = {

    val folderName: String = s"kafka_2.12-$kafkaVersion"
    val baseUrl: File = baseDir / s"$folderName.tgz"
    val kafkaHOME: File = baseDir / "kafka"

    if (!(baseDir / s"$folderName.tgz").exists()) {
      logger.info(s"Downloading $folderName...")
      (url(s"https://downloads.apache.org/kafka/$kafkaVersion/$folderName.tgz") #> baseUrl).!
    }

    if (!kafkaHOME.exists()) {
      FileTool.unzip(baseUrl, baseDir, isLinux)
      IO.move(baseDir / folderName, kafkaHOME)
      deleteDir(baseDir / folderName)
    }
  }

  private def doStart(log: ManagedLogger): Unit = {

    for {
      z <- Option(zookeeperPID.get())
      k <- Option(kafkaPID.get())
    } yield {

      z.doStart(new Callback {
        override def onComplete(): Unit = {
          k.doStart(new Callback {
            override def onComplete(): Unit = {
              log.info("Kafka started")
            }

            override def onError(): Unit = {}

            override def logger: ManagedLogger = log
          })
        }

        override def onError(): Unit = {}

        override def logger: ManagedLogger = log
      })
    }

  }

  private def doStop(log: ManagedLogger): Unit = {

    Option(kafkaPID.get()).foreach(_.doStop(log))
    Option(zookeeperPID.get()).foreach(_.doStop(log))
  }

  private def deleteDir(file: File): Unit = {
    val dir = new Directory(file)
    dir.deleteRecursively()
  }
}

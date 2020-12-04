package modux.plugin.kafka

import modux.plugin.ModuxPluginDefaults.moduxKafka
import modux.plugin.classutils.FileTool
import sbt.Keys._
import sbt.io.IO
import sbt._

import scala.sys.process._
import sbt.plugins.JvmPlugin
import sbt.{AutoPlugin, Def, ModuleID, Plugins, State, url}

import java.lang
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import scala.util.{Failure, Try}

object KafkaSupportPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires: Plugins = JvmPlugin

  object autoImport extends KafkaSupportSettings {
  }

  import autoImport._


  lazy val startupTransition: State => State = { s: State => "startKafka" :: s }
  lazy val stopTransition: State => State = { s: State => "stopKafka" :: s }

  private final val isLinux: Boolean = System.getProperty("os.name").toLowerCase match {
    case win if win.contains("win") => false
    case other if other.contains("mac") || other.contains("linux") => true
    case osName => throw new RuntimeException(s"Unknown operating system $osName")
  }

  private final val zookeeperPID = new AtomicReference[lang.Process]()
  private final val kafkaPID = new AtomicReference[lang.Process]()
  private final val baseDirRef = new AtomicReference[File]()

  lang.Runtime.getRuntime.addShutdownHook(new Thread(() => forceStop()))

  override lazy val projectSettings: Seq[Setting[_]] = Seq(

    kafkaVersion := "2.6.0",
    libraryDependencies ++= Seq(moduxKafka),
//    resolvers += "io.confluent" at "https://packages.confluent.io/maven/",
    onLoad in Global := {
      val old = (onLoad in Global).value
      startupTransition compose old
    },
    onUnload in Global := {
      val old = (onLoad in Global).value
      stopTransition compose old
    },
    startKafka := Def.task[Unit] {

      forceStop()

      val v: String = kafkaVersion.value
      val baseDir: File = target.value
      val kafkaHOME: File = baseDir / "kafka"
      val folderName = s"kafka_2.12-$v"
      val baseUrl: File = baseDir / s"$folderName.tgz"
      val logger = streams.value.log
      baseDirRef.set(baseDir)

      val firstStep: Try[Unit] = Try {
        if (!(baseDir / s"$folderName.tgz").exists()) {
          logger.info(s"Downloading $folderName...")
          (url(s"https://downloads.apache.org/kafka/$v/$folderName.tgz") #> baseUrl).!
        }
        if (!kafkaHOME.exists()){
          IO.delete(baseDir / folderName)
          FileTool.unzip(baseUrl, baseDir)
          IO.move(baseDir / folderName, kafkaHOME)
        }
      }

      val secondStep: Try[Unit] = {
        println("[info] Starting Kafka...")
        doStartServer("zookeeper-server-start.bat", "zookeeper.properties", "zookeeper", kafkaHOME, zookeeperPID)
        Thread.sleep(5000)
        doStartServer("kafka-server-start.bat", "server.properties", "kafka-server", kafkaHOME, zookeeperPID)
      }

      {
        for {
          _ <- firstStep
          _ <- secondStep
        } yield ()
      }.recover { case t =>
        t.printStackTrace()
        Failure(t)
      }
    }.value,

    stopKafka := Def.task[Unit] {
      forceStop()
    }
  )

  private def doStartServer(startScript: String, configFile: String, streamFileName: String, KAFKA_HOME: File, storeRef: AtomicReference[lang.Process]): Try[Unit] = Try {
    val workspace: File = if (isLinux) KAFKA_HOME / "bin" else KAFKA_HOME / "bin" / "windows"
    val absoluteScriptPath: String = (workspace / startScript).absolutePath
    val process: lang.Process = new lang.ProcessBuilder(absoluteScriptPath, s"../../config/$configFile")
      .directory(workspace)
      .redirectError(KAFKA_HOME / s"$streamFileName.error")
      .redirectOutput(KAFKA_HOME / s"$streamFileName.out")
      .start()

    storeRef.set(process)
  }

  private def doStopServer(script: String, baseDir: File): Unit = {
    val scriptLocal: String = if (isLinux) s"/$script.sh" else s"/windows/$script.bat"
    val absoluteFile: File = baseDir.getAbsoluteFile
    val p: lang.Process = new lang.ProcessBuilder(s"${absoluteFile.absolutePath}/bin$scriptLocal").directory(absoluteFile).start()

    p.waitFor(10, TimeUnit.SECONDS)
  }

  private def forceStop(): Try[Unit] = Try {
    val maybeBaseDir: Option[File] = Option(baseDirRef.get())
    val maybeZookeeperPID: Option[lang.Process] = Option(zookeeperPID.get())
    val maybeKafkaPID: Option[lang.Process] = Option(kafkaPID.get())

    maybeBaseDir.foreach { baseDir =>
      val basePath: File = baseDir / "kafka"
      doStopServer("zookeeper-server-stop", basePath)
      doStopServer("kafka-server-stop", basePath)
    }

    maybeZookeeperPID.foreach(_.destroy())
    maybeKafkaPID.foreach(_.destroy())

    if (maybeBaseDir.isDefined || maybeKafkaPID.isDefined || maybeZookeeperPID.isDefined) {
      println(s"[success]: Kafka stopped...")
    }
  }
}

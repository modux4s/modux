package modux.plugin.kafka.service

import modux.plugin.kafka.Callback
import sbt._
import sbt.internal.util.ManagedLogger

import java.io.{BufferedReader, File, FileInputStream, FileReader}
import java.lang
import java.util.Properties
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import scala.reflect.io.Directory

final case class ZookeeperService(isLinux: Boolean, basePath: File) {
  private val matcher: String = "binding to port"
  private val configFile = "zookeeper.properties"
  private val workspace: File = if (isLinux) basePath / "bin" else basePath / "bin" / "windows"
  private val conf: String = s"./config/$configFile"
  private val ext: String = if (isLinux) "sh" else "bat"
  private val processRef = new AtomicReference[lang.Process]()
  private val absolutePath: String = (workspace / s"zookeeper-server-start.$ext").absolutePath

  def doStart(callback: Callback): Unit = {
    doStop(callback.logger)

    val errorLog: String = "zookeeper.error"
    val serverLog: String = "zookeeper.out"
    val process: ProcessBuilder = new lang.ProcessBuilder(absolutePath, conf)
      .directory(basePath)
      .redirectError(basePath / errorLog)
      .redirectOutput(basePath / serverLog)

    processRef.set(process.start())

    val fr = new FileReader(basePath / serverLog)
    val br = new BufferedReader(fr)

    val run = new AtomicBoolean(true)
    val counter = new AtomicInteger(0)
    while (run.get && counter.get() < 1000) {
      val line: String = br.readLine()
      if (line == null) {
        Thread.sleep(1000)
      } else {
        if (line.contains(matcher)) {
          run.set(false)
        }
      }
      counter.getAndDecrement()
    }

    br.close()
    fr.close()

    if (counter.get() < 1000) {
      callback.onComplete()
    } else {
      callback.onError()
    }
  }

  def doStop(logger: ManagedLogger): Unit = {

    deleteTmp()
    Option(processRef.get()).foreach(_.destroy())

    val scriptLocal: String = if (isLinux) "/zookeeper-server-stop.sh" else "/windows/zookeeper-server-stop.bat"
    val stopScript: File = basePath / s"bin$scriptLocal"

    if (stopScript.exists()) {
      val process: Process = new ProcessBuilder(stopScript.absolutePath).directory(basePath).start()
      process.waitFor()
      process.destroy()
    }
  }

  private def deleteTmp(): Unit = {
    val props: Properties = new Properties()
    val confFile: File = basePath / "config" / configFile

    if (confFile.exists()) {
      val stream = new FileInputStream(confFile)
      props.load(stream)
      Option(props.getProperty("dataDir")).foreach { tmp =>
        val dir = new Directory(new File(tmp))
        dir.deleteRecursively()
      }
      stream.close()
    }
  }
}

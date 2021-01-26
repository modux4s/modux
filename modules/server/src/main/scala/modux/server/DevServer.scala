package modux.server

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import modux.server.model.Types.ExporterResolver
import modux.server.service.ModuxServer
import modux.shared.BuildContext
import org.slf4j.LoggerFactory
import org.slf4j.helpers.SubstituteLoggerFactory

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import scala.language.reflectiveCalls

private[modux] object DevServer {

  private val serverRef: AtomicReference[ModuxServer] = new AtomicReference[ModuxServer]()
  private var lastTimeLogFileModified: Long = 0

  def reload(buildContext: BuildContext): Unit = {
    resetLogger(buildContext)
    resetServer(buildContext)
  }

  def stop(): Unit = {
    Option(serverRef.get()).foreach(_.stop())
  }

  /**
   * Exports api to format json or yaml
   */
  def exporter(buildContext: BuildContext): String = {
    val m: Class[_] = buildContext.appClassloader.loadClass("modux.exporting.swagger.Exporter$")
    val exporter: ExporterResolver = m.getField("MODULE$").get(null).asInstanceOf[ExporterResolver]
    exporter.processor(buildContext)
  }

  private def resetLogger(buildContext: BuildContext): Boolean = {
    LoggerFactory.getILoggerFactory match {
      case context: LoggerContext =>

        Option(buildContext.appClassloader.getResource(buildContext.get("logger.file")))
          .exists { url =>

            val currentFile: File = new File(url.toURI)
            val lastTime: Long = currentFile.lastModified()

            val reload: Boolean = !Option(lastTimeLogFileModified).contains(lastTime)

            if (reload) {
              lastTimeLogFileModified = lastTime
              val joranConfigurator: JoranConfigurator = new JoranConfigurator
              joranConfigurator.setContext(context)
              context.reset()
              joranConfigurator.doConfigure(currentFile)
            }

            reload
          }

      case factory: SubstituteLoggerFactory =>
        factory.clear()
        true
      case _ => false
    }
  }

  private def resetServer(buildContext: BuildContext): Unit = {
    stop()
    start(buildContext)
  }

  private def start(buildContext: BuildContext): Unit = {
    serverRef.set(
      ModuxServer(
        buildContext.get("appName"),
        buildContext.get("host"),
        buildContext.get("port").toInt,
        buildContext.appClassloader
      )
    )
  }
}

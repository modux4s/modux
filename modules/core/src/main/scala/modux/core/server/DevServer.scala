package modux.core.server

import java.io.File

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import modux.core.exporter.Exporter
import modux.core.server.service.ModuxServer
import modux.shared.BuildContext
import org.slf4j.LoggerFactory
import org.slf4j.helpers.SubstituteLoggerFactory

private[modux] object DevServer {

  private var serverRef: ModuxServer = _
  private var lastTimeLogFileModified: Long = 0
  private var firstTime: Boolean = true

  def reload(buildContext: BuildContext): Unit = {

    if (!resetLogger(buildContext) || firstTime) {
      resetServer(buildContext)
      firstTime = false
    }
  }

  def stop(): Unit = {
    Option(serverRef).foreach(_.stop())
  }

  /**
   * Exports api to format json or yaml
   */
  def exporter(buildContext: BuildContext): String = {
    Exporter.processor(buildContext)
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
    serverRef = ModuxServer(
      buildContext.get("appName"),
      buildContext.get("host"),
      buildContext.get("port").toInt,
      buildContext.appClassloader
    )
  }
}

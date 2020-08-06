package modux.core.example

import modux.core.exporter.Exporter
import modux.shared.{BuildContext, ServerDecl, ServerVar}

object ExportTest extends App {


  import scala.collection.JavaConverters._

  val settings: java.util.HashMap[String, String] = new java.util.HashMap[String, String]
  settings.put("appName", "test")
  settings.put("host", "localhost")
  settings.put("port", "9000")
  settings.put("baseDirectory", "")
  settings.put("logger.file", "logback.xml")
  settings.put("project.description", "a test")
  settings.put("project.version", "0.1.0")

  val buildContext = BuildContext(
    settings,
    this.getClass.getClassLoader,
    Seq(
      ServerDecl(
        "{schema}://localhost:{port}",
        "Ambiente local",
        Map(
          "schema" -> ServerVar("https", "http", "https"),
          "port" -> ServerVar("9000", "9000", "9001"),
        )
      )
    ).asJava
  )
  println(Exporter.processor(buildContext))
}

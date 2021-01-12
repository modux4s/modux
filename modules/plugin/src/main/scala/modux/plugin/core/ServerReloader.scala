package modux.plugin.core

import java.io.File
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import modux.plugin.classutils.DelegatingClassLoader
import modux.plugin.core.Types.Server
import modux.shared.{BuildContext, ServerDecl, Threads}
import org.apache.xbean.classloader.NamedClassLoader
import sbt._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.reflectiveCalls
import scala.reflect.internal.util.ScalaClassLoader.URLClassLoader
import scala.util.Try

private[modux] case class ServerReloader private(
                                                  servers: Seq[ServerDecl],
                                                  settings: java.util.Map[String, String],
                                                  classpathDir: File,
                                                  resourceDir: Seq[File],
                                                  buildClassloader: ClassLoader,
                                                  dependencies: Seq[File],
                                                  moduxModules: Seq[File],
                                                  root: ClassLoader
                                                ) {

  private val appClassLoaderRef: AtomicReference[NamedClassLoader] = new AtomicReference[NamedClassLoader]()

  private lazy val baseClassloader: NamedClassLoader = {

    val delegator: DelegatingClassLoader = new DelegatingClassLoader(root, buildClassloader)
    val depsLoader: URLClassLoader = new URLClassLoader(Path.toURLs(dependencies ++ resourceDir), delegator)

    new NamedClassLoader(
      "mods-loader",
      Path.toURLs(moduxModules),
      depsLoader
    )
  }

  private val server: Server = {
    Threads.withContextClassLoader(baseClassloader) {
      import scala.language.reflectiveCalls
      val m: Class[_] = baseClassloader.loadClass("modux.server.DevServer$")
      m.getField("MODULE$").get(null).asInstanceOf[Server]
    }
  }

  def createAppLoader(): BuildContext = {

    val appLoader: NamedClassLoader = new NamedClassLoader(
      s"current-application-loader-${System.currentTimeMillis()}",
      Path.toURLs(Seq(classpathDir)),
      baseClassloader
    )

    BuildContext(settings, appLoader, servers.asJava)
  }

  def shutdown(): Unit = {
    server.stop()
  }

  def reload(): Unit = {
    destroyAppLoader()
    server.reload(createAppLoader())
  }

  def exporter(mode: String): String = {
    val buildContext: BuildContext = createAppLoader()
    buildContext.settings.put("export.mode", mode)

    server.exporter(buildContext)
  }

  private def destroyAppLoader(): Unit = {
    Option(appClassLoaderRef.get()).foreach { x =>
      Try(x.close())
      Try(x.destroy())
    }
  }
}

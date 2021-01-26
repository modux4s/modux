package modux.plugin.core

import modux.plugin.classutils.DelegatingClassLoader
import modux.plugin.core.Types.Server
import modux.shared.{BuildContext, ServerDecl, Threads}
import org.apache.xbean.classloader.{JarFileClassLoader, NamedClassLoader}
import sbt._

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import scala.collection.JavaConverters._
import scala.language.reflectiveCalls
import scala.util.Try

private[modux] case class ServerReloader private(
                                                  servers: Seq[ServerDecl],
                                                  settings: java.util.Map[String, String],
                                                  classpathDir: File,
                                                  resourceDir: Seq[File],
                                                  assetsDir: Seq[File],
                                                  buildClassloader: ClassLoader,
                                                  dependencies: Seq[File],
                                                  moduxModules: Seq[File],
                                                  root: ClassLoader
                                                ) {

  private val appClassLoaderRef: AtomicReference[NamedClassLoader] = new AtomicReference[NamedClassLoader]()
  private val assetsLoaderRef = new AtomicReference[JarFileClassLoader]()

  private val delegator: DelegatingClassLoader = new DelegatingClassLoader(root, buildClassloader)

  private val baseClassloader: NamedClassLoader = {

    val depsLoader: NamedClassLoader = new NamedClassLoader("deps-loader", Path.toURLs(dependencies), delegator)

    new NamedClassLoader(
      "mods-loader",
      Path.toURLs(moduxModules ++ resourceDir),
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

    val assetsLoader = new JarFileClassLoader("assets-app-loader", Path.toURLs(assetsDir), baseClassloader)
    assetsLoaderRef.set(assetsLoader)

    val appLoader: NamedClassLoader = new NamedClassLoader(
      s"current-application-loader-${System.currentTimeMillis()}",
      Path.toURLs(Seq(classpathDir)),
      assetsLoader
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

    Seq(Option(assetsLoaderRef.get()), Option(appClassLoaderRef.get()))
      .flatten
      .foreach { x =>
        Try(x.close())
        Try(x.destroy())
      }

  }
}

package modux.plugin.core

import modux.plugin.classutils.DelegatingClassLoader
import modux.plugin.core.Types.Server
import modux.shared.{BuildContext, ServerDecl, Threads}
import org.apache.xbean.classloader.{JarFileClassLoader, NamedClassLoader}
import sbt.*

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import scala.collection.JavaConverters.*
import scala.language.reflectiveCalls
import scala.util.Try

private[modux] case class ServerReloader private(
                                                  basePath: String,
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

  private val internalClassLoaderRef: AtomicReference[NamedClassLoader] = new AtomicReference[NamedClassLoader]()
  private val appClassLoaderRef: AtomicReference[NamedClassLoader] = new AtomicReference[NamedClassLoader]()
  private val assetsLoaderRef = new AtomicReference[JarFileClassLoader]()

  private val delegator: DelegatingClassLoader = new DelegatingClassLoader(root, buildClassloader)
  private val (dyncDep, fixedDeps) = dependencies.partition(x => x.absolutePath.startsWith(basePath))
  private val baseClassloader: NamedClassLoader = {

    val depsLoader: NamedClassLoader = new NamedClassLoader("deps-loader", Path.toURLs(fixedDeps), delegator)

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

    val dyncDepsLoader = new JarFileClassLoader("dync-deps-loader", Path.toURLs(dyncDep), baseClassloader)
    val assetsLoader = new JarFileClassLoader("assets-app-loader", Path.toURLs(assetsDir), dyncDepsLoader)
    val appLoader: NamedClassLoader = new NamedClassLoader(
      s"current-application-loader-${System.currentTimeMillis()}",
      Path.toURLs(Seq(classpathDir)),
      assetsLoader
    )
    internalClassLoaderRef.set(dyncDepsLoader)
    assetsLoaderRef.set(assetsLoader)
    appClassLoaderRef.set(appLoader)

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
    Seq(Option(assetsLoaderRef.get()), Option(appClassLoaderRef.get()), Option(internalClassLoaderRef.get()))
      .flatten
      .foreach { x =>
        Try(x.close())
        Try(x.destroy())
      }
  }
}

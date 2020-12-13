package modux.plugin

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.MappingsHelper._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import modux.plug.HookPlugin
import modux.plugin.ModuxPluginDefaults._
import modux.plugin.core.{InProgress, ModuxState, ModuxUtils, ServerReloader}
import modux.shared.PrintUtils
import sbt.Keys._
import sbt.nio.Keys._
import sbt.nio.Watch
import sbt.plugins.JvmPlugin
import sbt.util.CacheImplicits._
import sbt.util.CacheStore
import sbt.{Compile, Def, _}

import java.nio.file.{Path => JPath}
import java.util
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object ModuxPlugin extends AutoPlugin {

  private final val CONFIG_DIR: String = "conf"
  private final val store: CacheStore = sbt.util.CacheStore(file("./target/streams/modux"))
  private final val delayState: AtomicBoolean = new AtomicBoolean(false)
  private final val compilingState: AtomicBoolean = new AtomicBoolean(true)

  private def lastIsCompile(): Boolean = {
    store.read[Option[String]](None).contains("compile")
  }

  private def writeMode(s: String): Unit = {
    store.write(s)
  }

  override def trigger = allRequirements

  override def requires: Plugins = JvmPlugin && JavaAppPackaging && HookPlugin

  object autoImport extends ModuxPluginSettings {
  }

  import autoImport._
  import com.typesafe.sbt.packager.archetypes.JavaAppPackaging.autoImport._
  import modux.plug.HookPlugin.autoImport._

  val watchOnFileInputEventImpl: (Int, Watch.Event) => Watch.ContinueWatch = (_: Int, y: Watch.Event) => {
    val path: String = y.path.toFile.getAbsolutePath

    if (path.endsWith("~")) {
      Watch.Ignore
    } else {
      if (compilingState.get()) {
        if (delayState.get()) {
          Watch.Ignore
        } else {
          Watch.Trigger
        }
      } else {
        Watch.Ignore
      }
    }
  }

  val watchStartMessageImpl = Def.setting {
    (_: Int, _: ProjectRef, _: Seq[String]) => {
      Option(s"Server online at http://${moduxHost.value}:${moduxPort.value}/")
    }
  }

  val watchOnTerminationImpl: (Watch.Action, String, Int, State) => State = (_: Watch.Action, _: String, _: Int, state: State) => {

    val value: ModuxState = ModuxState.get
    if (value.initialized) {
      value.serverReloader.shutdown()
      ModuxState.clean()
    }

    val e = Project.extract(state)
    val (newState, _) = e.runTask(moduxStopHook, state)

    state.log.success("Server down")
    newState
  }

  val watchTriggeredMessageImpl: (Int, JPath, Seq[String]) => Option[String] = (_: Int, _: JPath, _: Seq[String]) => None

  val runningFlag = Def.task {
    sys.props.put(MODUX_MODE, MODE_COMPILE)
  }

  val taskCompile = Def.taskDyn {
    if (lastIsCompile()) {
      if (compilingState.get()) {
        Def.task[Unit] {
          compilingState.set(false)
          (compile in Compile).value
          compilingState.set(true)
          Future {
            delayState.set(true)
            Thread.sleep(2000)
            delayState.set(false)
          }
        }
      } else {
        Def.task[Unit] {}
      }
    } else {
      Def.task {
        writeMode("compile")
        streams.value.log.info("Cleaning...")
        val _ = (compile in Compile).dependsOn(clean).value
      }
    }
  }.dependsOn(runningFlag)

  val runnerImpl: Def.Initialize[InputTask[Unit]] = Def.inputTaskDyn {
    val moduxCurrentState: ModuxState = moduxState.value
    if (moduxCurrentState.isContinuous) {
      Def.task[Unit] {
        moduxCurrentState.serverReloader.reload()
      }
    } else {
      Def.task[Unit] {
        val streamValue: TaskStreams = streams.value
        moduxCurrentState.serverReloader.reload()
        (watchStartMessage in(Compile, run)).value(0, thisProjectRef.value, Nil).foreach(x => streamValue.log.info(x))
        ModuxUtils.waitForEnter(pollInterval.value.toMillis)
        (watchOnTermination in(Compile, run)).value(Watch.Ignore, "", 0, state.value)
      }
    }
  }.dependsOn(taskCompile)

  private val startHookImpl: Def.Initialize[Task[Unit]] = Def.taskDyn {
    val d: Seq[Def.Initialize[Task[Unit]]] = startHook.value
    if (d.nonEmpty) {
      Def.sequential(d)
    } else {
      Def.task[Unit] {}
    }
  }

  val createServer: Def.Initialize[Task[ServerReloader]] = Def.task {
    val depsClasspath: Classpath = (dependencyClasspath in(Compile, run)).value
    val cd: File = (classDirectory in(Compile, run)).value
    val rd: Seq[File] = Seq(
      (resourceDirectory in(Compile, run)).value,
      baseDirectory.value / CONFIG_DIR
    )

    val buildLoader: ClassLoader = this.getClass.getClassLoader
    val (persisted, deps) = depsClasspath.map(_.data).partition(x => x.getAbsolutePath.contains("modux"))

    val settings: util.Map[String, String] = new java.util.HashMap[String, String]
    settings.put("appName", name.value)
    settings.put("env", "develop")
    settings.put("host", moduxHost.value)
    settings.put("port", moduxPort.value.toString)
    settings.put("baseDirectory", baseDirectory.value.toString)
    settings.put("logger.file", moduxLogFile.value)
    settings.put("project.description", description.value)
    settings.put("project.version", version.value)

    contact.value.foreach(x => settings.put("project.contact", x))
    licenses.value.headOption.foreach { case (str, url) =>
      settings.put("project.license.name", str)
      settings.put("project.license.url", url.toString)
    }

    ServerReloader(
      servers.value,
      settings,
      cd,
      rd,
      buildLoader,
      deps,
      persisted.filterNot(x => x.absolutePath.contains("dev-server")),
      scalaInstance.value.loaderLibraryOnly
    )
  }

  val moduxStateImpl: Def.Initialize[Task[ModuxState]] = Def.taskDyn {

    val gs: ModuxState = ModuxState.get

    if (gs.initialized) {
      Def.task(gs)
    } else {

      Def.task {

        ModuxState.update(InProgress(state.value, createServer.value))
      }.dependsOn(startHookImpl)
    }
  }

  private val exportTask: Def.Initialize[Task[ServerReloader]] = Def.task {
    val _ = (compile in Compile).value
    createServer.value
  }

  private val exportFlags = Def.task {
    sys.props.put(MODUX_MODE, MODE_EXPORT)
  }

  private def saveExport(mode: String): Def.Initialize[Task[Unit]] = {
    val cleanReq = Def.taskDyn {
      if (lastIsCompile()) {
        Def.task[Unit] {
          streams.value.log.info("Cleaning...")
          clean.value
        }
      } else {
        Def.task[Unit] {
          streams.value.log.info("Not cleaning required...")
        }
      }
    }.dependsOn(exportFlags)

    Def.task {
      writeMode("export")
      val data: String = exportTask.value.exporter(mode)
      val file: File = target.value / "api" / s"${name.value}.$mode"
      IO.write(file, data)
    }.dependsOn(cleanReq)
  }

  override lazy val globalSettings: Seq[Def.Setting[_]] = Seq(
    useSuperShell := false,
    resolvers += Resolver.bintrayRepo("jsoft", "maven")
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    contact := None,
    servers := Nil,
    moduxPort := 9000,
    moduxHost := "localhost",
    moduxState := moduxStateImpl.value,
    moduxLogFile := "logback.xml",
    moduxExportYaml := saveExport("yaml").value,
    moduxExportJson := saveExport("json").value,
    moduxOpenAPIVersion := 3,
    mappings in Universal ++= directory(CONFIG_DIR),
    resolvers += Resolver.mavenLocal,
    libraryDependencies ++= Def.setting {
      if (moduxOpenAPIVersion.value == 2) {
        Seq(moduxServer, moduxOpenAPIV2)
      } else {
        Seq(moduxServer, moduxOpenAPIV3)
      }
    }.value,
    mainClass in Compile := Option("modux.core.server.ProdServer"),
    mainClass in(Compile, run) := Option("modux.core.server.DevServer"),
    run in Compile := runnerImpl.evaluated,
    watchStartMessage in(Compile, run) := watchStartMessageImpl.value,
    watchPersistFileStamps in(Compile, run) := true,
    watchAntiEntropy in(Compile, run) := FiniteDuration(2, TimeUnit.SECONDS),
    watchForceTriggerOnAnyChange in(Compile, run) := false,
    watchTriggers in(Compile, run) += baseDirectory.value.toGlob / CONFIG_DIR / **,
    watchOnTermination in(Compile, run) := watchOnTerminationImpl,
    watchTriggeredMessage in(Compile, run) := watchTriggeredMessageImpl,
    watchOnFileInputEvent in(Compile, run) := watchOnFileInputEventImpl,
    scriptClasspath := Seq("*", s"../$CONFIG_DIR"),
    javaOptions in Universal := Def.task {
      Seq(
        s"-Dmodux.host=${moduxHost.value}",
        s"-Dmodux.port=${moduxPort.value}",
        s"-Dmodux.name=${name.value}",
      )
    }.value,

    moduxStopHook := Def.taskDyn[Unit] {
      val value: Seq[Def.Initialize[Task[Unit]]] = stopHook.value
      if (value.nonEmpty) {
        Def.sequential(value)
      } else {
        Def.task {}
      }
    }.value
  )
}

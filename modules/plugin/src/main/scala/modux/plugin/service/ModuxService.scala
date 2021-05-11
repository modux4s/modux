package modux.plugin.service

import com.typesafe.sbt.SbtNativePackager.Universal
import com.typesafe.sbt.packager.Keys.scriptClasspath
import com.typesafe.sbt.packager.MappingsHelper.directory
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.web.Import.Assets
import com.typesafe.sbt.web.SbtWeb
import modux.plug.HookPlugin
import modux.plugin.core._
import modux.shared.PrintUtils
import sbt.Keys._
import sbt.nio.Keys._
import sbt.nio.Watch
import sbt.plugins.JvmPlugin
import sbt.util.CacheStore
import sbt.util.StampedFormat._
import sbt.{AutoPlugin, Command, Def, Plugins, Project, ProjectRef, State, Task, file, _}

import java.io.File
import java.nio.file.{Path => JPath}
import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

object ModuxService extends AutoPlugin {
  private final val store: CacheStore = sbt.util.CacheStore(file("./target/streams/modux"))
  private final val timestamp = new AtomicLong(System.nanoTime())
  private final val withError = new AtomicBoolean(false)
  private def lastIsCompile(): Boolean = {
    store.read[Option[String]](None).contains("compile")
  }

  private def writeMode(s: String): Unit = {
    store.write(s)
  }

  override def requires: Plugins = JvmPlugin && JavaAppPackaging && HookPlugin && SbtWeb

  object autoImport extends ModuxServiceSettings {
  }

  import autoImport._
  import modux.plug.HookPlugin.autoImport._

  val watchOnFileInputEventImpl: (Int, Watch.Event) => Watch.ContinueWatch = (_: Int, y: Watch.Event) => {
    val path: String = y.path.toFile.getAbsolutePath
    if (path.endsWith("~")) {
      Watch.Ignore
    } else {
      val current: Long = System.nanoTime()
      if (current - timestamp.get() > 1000) {
        timestamp.set(current)
        Watch.Trigger
      } else {
        Watch.Ignore
      }
    }
  }

  private val watchStartMessageImpl = Def.setting {
    { (_: Int, _: ProjectRef, _: Seq[String]) =>
      if (withError.get()) {
        PrintUtils.error("[error] Modux server is down. Fix errors first.")
      } else {
        PrintUtils.success(s"Server online at http://${moduxHost.value}:${moduxPort.value}/")
      }

      None
    }
  }

  private val watchOnTerminationImpl: (Watch.Action, String, Int, State) => State = (_: Watch.Action, _: String, _: Int, state: State) => {

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

  val runningFlag: Def.Initialize[Task[Unit]] = Def.task {
    sys.props.put(MODUX_MODE, MODE_COMPILE)
  }

  val taskCompile: Def.Initialize[Task[Unit]] = Def.sequential(
    runningFlag,
    Def.taskDyn {
      //      withError.set(false)
      {
        if (lastIsCompile()) {
          Def.sequential(
            Assets / packageBin,
            Compile / compile,
          )
        } else {
          Def.sequential(
            streams.map(_.log.info("Cleaning...")),
            clean,
            Def.task(writeMode("compile")),
            Assets / packageBin,
            Compile / compile,
          )
        }
      }.map(_ => withError.set(false))
    }
  )

  val runnerImpl = Def.inputTaskDyn[Unit] {

    val moduxCurrentState: ModuxState = moduxState.value
    if (moduxCurrentState.isContinuous) {
      Def.task[Unit] {
        moduxCurrentState.serverReloader.reload()
      }
    } else {
      Def.task[Unit] {
        val streamValue: TaskStreams = streams.value
        moduxCurrentState.serverReloader.reload()
        (Compile / run / watchStartMessage).value(0, thisProjectRef.value, Nil).foreach(x => streamValue.log.info(x))
        ModuxUtils.waitForEnter()
        (Compile / run / watchOnTermination).value(Watch.Ignore, "", 0, state.value)
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
    val depsClasspath: Classpath = (Compile / run / dependencyClasspath).value
    val cd: File = (Compile / run / classDirectory).value
    val rd: Seq[File] = (Compile / run / resourceDirectories).value
    val assetsDir = Seq((Assets / packageBin).value)
    val rootDir: String = new File(loadedBuild.value.root).absolutePath

    val buildLoader: ClassLoader = this.getClass.getClassLoader
    val (persisted, deps) = depsClasspath.map(_.data).partition(x => x.getAbsolutePath.contains("modux"))

    val settings: util.Map[String, String] = new java.util.HashMap[String, String]
    settings.put("appName", name.value)
    settings.put("env", "develop")
    settings.put("host", moduxHost.value)
    settings.put("port", moduxPort.value.toString)
    settings.put("baseDirectory", baseDirectory.value.toString)
    settings.put("rootDirectory", rootDir)
    settings.put("logger.file", moduxLogFile.value)
    settings.put("project.description", description.value)
    settings.put("project.version", version.value)

    contact.value.foreach(x => settings.put("project.contact", x))
    licenses.value.headOption.foreach { case (str, url) =>
      settings.put("project.license.name", str)
      settings.put("project.license.url", url.toString)
    }

    ServerReloader(
      rootDir,
      servers.value,
      settings,
      cd,
      rd,
      assetsDir,
      buildLoader,
      deps,
      persisted.filterNot(x => x.absolutePath.contains("dev-server")),
      scalaInstance.value.loaderLibraryOnly,
    )
  }

  val moduxStateImpl: Def.Initialize[Task[ModuxState]] = Def.taskDyn {

    val gs: ModuxState = ModuxState.get

    if (gs.initialized) {
      Def.task(gs)
    } else {

      Def.sequential(
        startHookImpl,
        Def.task {
          PrintUtils.cyan("""___________________________________________________""")
          PrintUtils.cyan("""      _______  _____  ______  _     _ _     _      """)
          PrintUtils.cyan("""      |  |  | |     | |     \ |     |  \___/       """)
          PrintUtils.cyan("""      |  |  | |_____| |_____/ |_____| _/   \_      """)
          PrintUtils.cyan("""---------------------------------------------------""")
          ModuxState.update(InProgress(state.value, createServer.value))
        }
      )
    }
  }

  private val exportTask: Def.Initialize[Task[ServerReloader]] = Def.sequential(
    Compile / compile,
    createServer
  )

  private val exportFlags = Def.task {
    sys.props.put(MODUX_MODE, MODE_EXPORT)
  }

  private val stopModuxServerCommand = Command.command("stopModuxServer") { state =>

    val value: ModuxState = ModuxState.get
    if (value.initialized) {
      value.serverReloader.shutdown()
      ModuxState.clean()
    }

    val e = Project.extract(state)
    val (newState, _) = e.runTask(moduxStopHook, state)

    state.log.success("Server down")
    newState.setInteractive(false)
  }

  private def saveExport(mode: String): Def.Initialize[Task[Unit]] = {
    val cleanReq = Def.taskDyn {
      if (lastIsCompile()) {
        Def.sequential(
          exportFlags,
          streams.map(_.log.info("Cleaning...")),
          clean
        )
      } else {
        Def.sequential(
          exportFlags,
          streams.map(_.log.info("Not cleaning required..."))
        )
      }
    }

    Def.sequential(
      cleanReq,
      Def.task {
        writeMode("export")
        val data: String = exportTask.value.exporter(mode)
        val file: File = target.value / "api" / s"${name.value}.$mode"
        IO.write(file, data)
      }
    )
  }

  override def globalSettings: Seq[Def.Setting[_]] = Seq(
    useSuperShell := false
  )

  override def projectSettings: Seq[Setting[_]] = Seq(
    contact := None,
    servers := Nil,
    moduxPort := 9000,
    moduxHost := "localhost",
    moduxState := moduxStateImpl.value,
    moduxLogFile := "logback.xml",
    moduxExportYaml := saveExport("yaml").value,
    moduxExportJson := saveExport("json").value,
    moduxOpenAPIVersion := 3,
    Universal / mappings ++= directory("conf"),
    commands ++= Seq(stopModuxServerCommand),

    libraryDependencies ++= Def.setting {
      if (moduxOpenAPIVersion.value == 2) {
        Seq(CommonSettings.moduxServer, CommonSettings.moduxOpenAPIV2)
      } else {
        Seq(CommonSettings.moduxServer, CommonSettings.moduxOpenAPIV3)
      }
    }.value,

    Compile / mainClass := Option("modux.server.ProdServer"),
    Compile / run / mainClass := Option("modux.server.DevServer"),
    run := runnerImpl.evaluated,
    run / watchLogLevel := sbt.util.Level.Error,
    run / watchStartMessage := watchStartMessageImpl.value,
    run / watchInputOptions := Seq(
      Watch.InputOption("<enter>", "Stop modux server", Watch.Run("stopModuxServer"), '\n', '\r', 4.toChar)
    ),
    run / watchTriggers += baseDirectory.value.toGlob / ** / "*.properties",
    run / watchTriggers += baseDirectory.value.toGlob / ** / "*.conffiref",
    run / watchTriggers += baseDirectory.value.toGlob / ** / "*.xml",
    run / watchTriggers += baseDirectory.value.toGlob / ** / "*.scala.*",
    run / watchOnTermination := watchOnTerminationImpl,
    run / watchTriggeredMessage := watchTriggeredMessageImpl,
    run / watchOnFileInputEvent := watchOnFileInputEventImpl,
    run / watchBeforeCommand := {
      () => withError.set(true)
    },
    scriptClasspath := Seq("*", s"../conf"),
    Universal / javaOptions := Def.task {
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

package modux.server.service

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem => ClassicAS}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.concat
import akka.http.scaladsl.server.{Directives, Route}
import com.softwaremill.macwire.Wired
import com.typesafe.config.{Config, ConfigFactory}
import modux.core.api.ModuleX
import modux.macros.di.MacwireSupport
import modux.model.context.{Context, ContextInject}
import modux.model.{ServiceDef, ServiceEntry}
import modux.server.lib.{ConfigBuilder, RouterCapture}
import modux.server.model
import modux.server.model.Capture
import modux.shared.PrintUtils
import org.slf4j.{Logger, LoggerFactory}

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success, Try}

case class ModuxServer(appName: String, host: String, port: Int, appClassloader: ClassLoader) {

  private final val DEFAULT_DURATION: FiniteDuration = Duration(5, TimeUnit.SECONDS)

  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private lazy val binding: AtomicReference[Http.ServerBinding] = new AtomicReference[Http.ServerBinding]()

  private val configStr: String = ConfigBuilder.build(appName)
  private val localConfig: Config = ConfigFactory.defaultApplication(appClassloader).withFallback(ConfigFactory.parseString(configStr))
  private implicit val sys: ClassicAS = ClassicAS(appName, Option(localConfig), Option(appClassloader), None)
  private val sysTyped: ActorSystem[Nothing] = sys.toTyped
  private implicit val ec: ExecutionContextExecutor = sysTyped.executionContext

  private val context: Context = new Context {
    override val applicationLoader: ClassLoader = appClassloader
    override val applicationName: String = appName
    override val actorSystem: ActorSystem[Nothing] = sysTyped
    override val config: Config = localConfig
    override val executionContext: ExecutionContext = ec
  }

  ContextInject.setInstance(context)

  val capture: Capture = context.contextThread(captureCall(context, localConfig))

  Http().newServerAt(host, port).bindFlow(capture.routes).onComplete {
    case Failure(exception) => logger.error(exception.getLocalizedMessage, exception)
    case Success(value) => binding.set(value)
  }

  def stop(): Unit = context.contextThread {

    Option(binding.get()).foreach(value => Await.result(value.terminate(DEFAULT_DURATION), DEFAULT_DURATION))
    terminateSys()
  }

  private def terminateSys(): Unit = {

    capture
      .modules
      .foreach { mod =>
        Try(mod.onStop()).recover { case t => logger.error(t.getLocalizedMessage, t) }
      }

    capture.servicesEntry.foreach { x =>
      Try(x.onStop()).recover { case t => logger.error(t.getLocalizedMessage, t) }
    }

    sys.terminate()
    Await.result(sys.whenTerminated, DEFAULT_DURATION)
  }

  private def captureCall(context: Context, localConfig: Config): Capture = {

    import com.github.andyglow.config._
    val wired: Wired = MacwireSupport.wiredInModule(context)
    val routes: mutable.ArrayBuffer[Route] = mutable.ArrayBuffer.empty
    val modules: mutable.ArrayBuffer[ModuleX] = mutable.ArrayBuffer.empty
    val specs: mutable.ArrayBuffer[ServiceDef] = mutable.ArrayBuffer.empty
    val serviceEntry: mutable.ArrayBuffer[ServiceEntry] = mutable.ArrayBuffer.empty

    //************** CACHE **************//

    localConfig
      .getOrElse[List[String]]("modux.modules", Nil)
      .flatMap { packageStr =>
        Try(wired.wireClassInstanceByName(packageStr).asInstanceOf[ModuleX]) match {
          case Failure(exception) =>
            logger.error(exception.getLocalizedMessage, exception)
            None
          case Success(value) => Option(value)
        }
      }
      .zipWithIndex
      .foreach { case (moduleX, idx) =>

        modules.append(moduleX)

        val idxFinal: Int = idx + 1

        Try(moduleX.onStart()) match {
          case Failure(exception) =>
            logger.error(exception.getLocalizedMessage, exception)
            PrintUtils.error(s"${moduleX.getClass.getSimpleName} failing.")
          case _ =>

            PrintUtils.cyan(s"$idxFinal. ${moduleX.getClass.getSimpleName}")

            moduleX
              .providers
              .zipWithIndex
              .foreach { case (srv, idy) =>

                val serviceSpec: ServiceDef = srv.serviceDef

                specs.append(serviceSpec)

                serviceSpec.serviceEntries.foreach { entry =>
                  serviceEntry.append(entry)
                  Try(entry.onStart()).recover { case t => logger.error(t.getLocalizedMessage, t) }
                }

                PrintUtils.cyan(s"\t$idxFinal.${idy + 1} ${serviceSpec.name} OK!")

                routes += concat(RouterCapture.extract(serviceSpec.serviceEntries): _*)
              }
        }
      }

    model.Capture(Directives.concat(routes: _*), modules, specs, serviceEntry)
  }
}

package modux.core.server.service

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem => ClassicAS}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import com.typesafe.config.{Config, ConfigFactory}
import modux.core.api.ModuleX
import modux.core.server.domain.Capture
import modux.model.context.Context
import modux.model.dsl.RestEntry
import modux.model.{RestInstance, ServiceDescriptor}
import modux.shared.PrintUtils
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success, Try}

case class ModuxServer(appName: String, host: String, port: Int, appClassloader: ClassLoader) {

  private final val DEFAULT_DURATION: FiniteDuration = Duration(5, TimeUnit.SECONDS)

  private lazy val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private lazy val binding: AtomicReference[Http.ServerBinding] = new AtomicReference[Http.ServerBinding]()

  private val localConfig: Config = ConfigFactory.load(appClassloader)
  private implicit val sys: ClassicAS = ClassicAS(appName, Option(localConfig), Option(appClassloader), None)
  private val sysTyped: ActorSystem[Nothing] = sys.toTyped
  private implicit val ec: ExecutionContextExecutor = sysTyped.executionContext

  private val context: Context = new Context {
    override val actorSystem: ActorSystem[Nothing] = sysTyped
    override val config: Config = localConfig
    override val executionContext: ExecutionContext = ec
  }

  val capture: Capture = captureCall(context, localConfig)

  Http().newServerAt( host, port).bindFlow(capture.routes).onComplete {
    case Failure(exception) => logger.error(exception.getLocalizedMessage, exception)
    case Success(value) => binding.set(value)
  }

  def stop(): Unit = {

    Option(binding.get()).foreach(value => Await.result(value.terminate(DEFAULT_DURATION), DEFAULT_DURATION))
    terminateSys()
  }

  private def terminateSys(): Unit = {
    capture
      .modules
      .foreach { mod =>
        Try(mod.onStop()) match {
          case Failure(exception) => logger.error(exception.getLocalizedMessage, exception)
          case _ =>
        }
      }

    sys.terminate()
    Await.result(sys.whenTerminated, DEFAULT_DURATION)
  }

  private def captureCall(context: Context, localConfig: Config): Capture = {
    import com.github.andyglow.config._
    val routes: mutable.ArrayBuffer[Route] = mutable.ArrayBuffer.empty
    val modules: mutable.ArrayBuffer[ModuleX] = mutable.ArrayBuffer.empty
    val specs: mutable.ArrayBuffer[ServiceDescriptor] = mutable.ArrayBuffer.empty

    //************** CACHE **************//

    localConfig
      .get[List[String]]("modux.modules")
      .map(x => appClassloader.loadClass(x))
      .map(c => c.getConstructor(classOf[Context]).newInstance(context).asInstanceOf[ModuleX])
      .foreach { x =>

        modules.append(x)

        Try(x.onStart()) match {
          case Failure(exception) =>
            logger.error(exception.getLocalizedMessage, exception)
            PrintUtils.error(s"${x.getClass.getSimpleName} failing.")
          case _ =>

            x.providers.foreach { srv =>
              val serviceSpec: ServiceDescriptor = srv.serviceDescriptor
              specs.append(serviceSpec)
              PrintUtils.info(s"\tLoading ${serviceSpec.name}")

              val route: Route = {
                import akka.http.scaladsl.server.Directives.{concat, pathPrefix}
                val tmp: Route = concat(
                  serviceSpec.servicesCall.flatMap {
                    case x: RestEntry =>
                      x.instance match {
                        case instance: RestInstance => Option(instance.route)
                        case _ => None
                      }
                    case _ => None
                  }: _*
                )

                serviceSpec.namespace match {
                  case Some(value) => pathPrefix(value)(tmp)
                  case None => tmp
                }
              }

              routes += route
            }

            PrintUtils.info(s"${x.getClass.getSimpleName} active.")
        }
      }

    Capture(Directives.concat(routes: _*), modules, specs)
  }
}

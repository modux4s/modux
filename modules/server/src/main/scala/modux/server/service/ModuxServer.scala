package modux.server.service

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem => ClassicAS}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.CircuitBreaker
import com.typesafe.config.{Config, ConfigFactory}
import modux.core.api.ModuleX
import modux.model.context.Context
import modux.model.dsl.RestEntry
import modux.model.{RestInstance, ServiceDef}
import modux.server.model
import modux.server.model.Capture
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

  private val localConfig: Config = ConfigFactory.parseString(ModuxServer(appName)).withFallback(ConfigFactory.load(appClassloader))
  private implicit val sys: ClassicAS = ClassicAS(appName, Option(localConfig), Option(appClassloader), None)
  private val sysTyped: ActorSystem[Nothing] = sys.toTyped
  private implicit val ec: ExecutionContextExecutor = sysTyped.executionContext

  private val context: Context = new Context {
    override val actorSystem: ActorSystem[Nothing] = sysTyped
    override val config: Config = localConfig
    override val executionContext: ExecutionContext = ec
  }

  val capture: Capture = captureCall(context, localConfig)

  Http().newServerAt(host, port).bindFlow(capture.routes).onComplete {
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
    val specs: mutable.ArrayBuffer[ServiceDef] = mutable.ArrayBuffer.empty

    //************** VALS **************//
    lazy val maxFailure: Int = localConfig.get[Int]("modux.circuit-breaker.maxFailure")
    lazy val callTimeoutDur: Duration = Duration(localConfig.get[String]("modux.circuit-breaker.callTimeout"))
    lazy val resetTimeoutDut: Duration = Duration(localConfig.get[String]("modux.circuit-breaker.resetTimeout"))
    lazy val callTimeout: FiniteDuration = FiniteDuration(callTimeoutDur.toMillis, callTimeoutDur.unit)
    lazy val resetTimeout: FiniteDuration = FiniteDuration(resetTimeoutDut.toMillis, resetTimeoutDut.unit)
    //************** CACHE **************//

    localConfig
      .get[List[String]]("modux.modules")
      .map(x => appClassloader.loadClass(x))
      .map(c => c.getConstructor(classOf[Context]).newInstance(context).asInstanceOf[ModuleX])
      .zipWithIndex
      .foreach { case (x, idx) =>

        modules.append(x)

        val idxFinal: Int = idx + 1

        Try(x.onStart()) match {
          case Failure(exception) =>
            logger.error(exception.getLocalizedMessage, exception)
            PrintUtils.error(s"${x.getClass.getSimpleName} failing.")
          case _ =>

            PrintUtils.cyan(s"$idxFinal. ${x.getClass.getSimpleName}")

            x.providers.zipWithIndex.foreach { case (srv, idy) =>

              val serviceSpec: ServiceDef = srv.serviceDef

              specs.append(serviceSpec)

              PrintUtils.cyan(s"\t$idxFinal.${idy + 1} ${serviceSpec.name} OK!")

              val route: Route = {
                import akka.http.scaladsl.server.Directives.{concat, pathPrefix}
                val tmp: Route = concat(
                  serviceSpec.servicesCall.flatMap {
                    case x: RestEntry =>
                      x.instance match {
                        case instance: RestInstance => Option(instance.route(x._extensions))
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
        }
      }

    model.Capture(Directives.concat(routes: _*), modules, specs)
  }
}

object ModuxServer {
  def apply(appName: String): String = {
    s"""
       |
       |modux{
       |  modules = []
       |}
       |
       |akka {
       |  loggers = ["akka.event.slf4j.Slf4jLogger"]
       |
       |  loglevel = "info"
       |  stdout-loglevel = "off"
       |
       |  actor {
       |    provider = "cluster"
       |    allow-java-serialization = on
       |
       |    serializers {
       |      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
       |    }
       |
       |    serialization-bindings {
       |      "java.io.Serializable" = kryo
       |    }
       |  }
       |
       |  http {
       |    server {
       |      websocket {
       |        periodic-keep-alive-max-idle = 1 second
       |      }
       |    }
       |  }
       |
       |  remote.artery {
       |    canonical.port = 2553
       |    canonical.hostname = localhost
       |  }
       |
       |  cluster {
       |    seed-nodes = [
       |      "akka://$appName@localhost:2553"
       |    ]
       |
       |    sharding {
       |      number-of-shards = 100
       |    }
       |
       |    downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
       |  }
       |}
       |""".stripMargin
  }
}
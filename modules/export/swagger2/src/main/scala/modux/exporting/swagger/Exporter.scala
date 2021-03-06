package modux.exporting.swagger

import akka.actor.ActorSystem
import com.github.andyglow.config._
import com.softwaremill.macwire.Wired
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import io.swagger.jaxrs.Reader
import io.swagger.models._
import io.swagger.models.parameters.{CookieParameter, HeaderParameter, Parameter}
import io.swagger.models.properties.StringProperty
import io.swagger.models.utils.PropertyModelConverter
import io.swagger.util.{Json, Yaml}
import modux.core.api.ModuleX
import modux.macros.di.MacwireSupport
import modux.model.context.{Context, ContextInject}
import modux.model.dsl._
import modux.model.rest.{PathMetadata, RestProxy}
import modux.model.schema.MSchema
import modux.shared.BuildContext

import java.util
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object Exporter extends LazyLogging {

  def processor(buildContext: BuildContext): String = {

    def genContext(buildContext: BuildContext): Context = {
      val appClassloader: ClassLoader = buildContext.appClassloader
      val localConfig: Config = ConfigFactory.load(appClassloader)
      val sys: ActorSystem = ActorSystem(buildContext.get("appName"), Option(localConfig), Option(appClassloader), None)

      new Context {
        override def applicationLoader: ClassLoader = appClassloader

        override def applicationName: String = buildContext.settings.get("appName")

        override def classicActorSystem: ActorSystem = sys

        override def config: Config = localConfig

        override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
      }
    }

    def extractModules(context: Context): Seq[ModuleX] = context.contextThread {

      val wired: Wired = MacwireSupport.wiredInModule(context)

      context
        .config
        .get[List[String]]("modux.modules")
        .flatMap { packageStr =>
          Try(wired.wireClassInstanceByName(packageStr).asInstanceOf[ModuleX]) match {
            case Failure(exception) =>
              logger.error(exception.getLocalizedMessage, exception)
              None
            case Success(value) => Option(value)
          }
        }
    }

    def processModule(moduleX: ModuleX, swagger: Swagger): Map[String, Path] = {

      val propertyModelConverter: PropertyModelConverter = new PropertyModelConverter

      def add(map: Map[String, MSchema]): Unit = {
        map.foreach { case (k, v) =>
          swagger.addDefinition(k, propertyModelConverter.propertyToModel(VersionAdapter2(v)))
        }
      }

      moduleX
        .providers
        .flatMap { srv =>

          srv.serviceDef.serviceEntries
            .collect {
              case x: RestEntry => x
              case x: NameSpacedEntry => x
            }
            .flatMap {
              case x: NameSpacedEntry =>
                x
                  .entries
                  .collect { case y: RestEntry => (y, y.restService) }
                  .collect { case (z, y: RestProxy) => (z, y.copy(x.namespace + y.path)) }

              case x: RestEntry =>
                x.restService match {
                  case proxy: RestProxy => Seq((x, proxy))
                  case _ => Nil
                }
              case _ => Nil
            }
            .filterNot { case (_, x) => x.ignore }
            .groupBy(_._2.path)
            .map { case (path, xs) =>
              (
                PathMetadata.swaggerFormat(PathMetadata.normalizePath(path)),
                xs.foldLeft(new Path) { case (acc, (entry, proxy)) =>

                  val operation: Operation = new Operation
                  operation.consumes(entry._consumes.asJava)

                  entry._description.foreach(operation.setDescription)

                  //************** parameters **************//

                  val paramDesMap: Map[String, ParamDescriptor] = entry._paramDescriptor.map { x => x._kind.name -> x }.toMap
                  val cookieAndHeaderParams: Seq[Parameter] = entry._paramDescriptor.map(_._kind).toList.collect {
                    case x: CookieKind =>
                      val cookieParam: CookieParameter = new CookieParameter
                      cookieParam.setName(x.name)
                      cookieParam.setProperty(new StringProperty())
                      cookieParam
                    case x: HeaderKind =>
                      val headerParam: HeaderParameter = new HeaderParameter
                      headerParam.setProperty(new StringProperty)
                      headerParam.setName(x.name)
                      headerParam
                  }

                  val params: Seq[Parameter] = (VersionAdapter2(proxy.pathParameter) ++ VersionAdapter2(proxy.queryParameter) ++ cookieAndHeaderParams)
                    .map { p =>
                      paramDesMap.get(p.getName).fold(p) { x =>
                        p.setDescription(x._description.orNull)
                        p
                      }
                    }

                  val ls: java.util.List[Parameter] = new java.util.ArrayList[Parameter]()
                  val collection: util.Collection[Parameter] = params.asJavaCollection
                  ls.addAll(collection)

                  if (!ls.isEmpty) {
                    operation.setParameters(ls)
                  }
                  //************** request **************//
                  entry._expect.foreach { requestDescriptor =>
                    add(requestDescriptor.mediaType.schemaDescriptor.references)
                  }
                  //************** response **************//

                  entry._response.foreach { x =>
                    val response: Response = new Response
                    response.setDescription(x.description)
                    x.schema
                      .map(_.schemaDescriptor.reference)
                      .foreach { x =>
                        val model: Model = propertyModelConverter.propertyToModel(VersionAdapter2(x))
                        response.setResponseSchema(model)
                      }
                    x._example.asMap.foreach { case (k, v) => response.example(k, v) }

                    operation.addResponse(x.code.toString, response)
                  }

                  proxy.method match {
                    case "get" => acc.setGet(operation)
                    case "post" => acc.setPost(operation)
                    case "delete" => acc.setDelete(operation)
                    case "put" => acc.setPut(operation)
                    case _ =>
                  }

                  acc
                }
              )
            }
        }.toMap
    }

    def createInfo(buildContext: BuildContext, swagger: Swagger): Unit = {
      val info: Info = new Info

      info.setTitle(buildContext.get("appName"))
      info.setDescription(buildContext.get("project.description"))
      info.setVersion(buildContext.get("project.version"))

      for {
        name <- Option(buildContext.get("project.license.name"))
        url <- Option(buildContext.get("project.license.url"))
      } yield {
        val licence: License = new License
        licence.setName(name)
        licence.setUrl(url)
        info.setLicense(licence)
      }

      swagger.setInfo(info)
    }

    def processServers(buildContext: BuildContext, swagger: Swagger): Unit = {

      buildContext.servers.asScala.headOption.foreach { server =>
        swagger.setHost(server.url)
      }
    }

    def main(buildContext: BuildContext, context: Context): String = {

      val swagger: Swagger = new Swagger

      createInfo(buildContext, swagger)
      processServers(buildContext, swagger)
      extractModules(context)
        .flatMap { mod => processModule(mod, swagger) }
        .foreach { case (k, path) => swagger.path(k, path) }

      val reader: Reader = new Reader(swagger)

      val set: Set[Class[_]] = Set.empty
      val out: Swagger = reader.read(set.asJava)

      buildContext.get("export.mode") match {
        case "json" =>
          Json.pretty(out)
        case _ =>
          Yaml.pretty.writeValueAsString(out)
      }
    }

    val context: Context = genContext(buildContext)
    ContextInject.setInstance(context)
    val result: String = main(buildContext, context)
    context.actorSystem.terminate()
    result
  }
}

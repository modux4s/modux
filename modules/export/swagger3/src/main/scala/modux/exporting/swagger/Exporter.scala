package modux.exporting.swagger

import akka.actor.typed.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import modux.model.context.Context
import modux.shared.BuildContext
import akka.actor.{BootstrapSetup, ActorSystem => Classic}
import akka.actor.typed.scaladsl.adapter._
import com.github.andyglow.config._
import io.swagger.v3.core.util.{Json, Yaml}
import io.swagger.v3.jaxrs2.Reader
import io.swagger.v3.oas.integration.SwaggerConfiguration
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.info.{Info, License}
import io.swagger.v3.oas.models.media.{Content, MediaType, Schema, StringSchema}
import io.swagger.v3.oas.models.parameters.{CookieParameter, HeaderParameter, Parameter, RequestBody}
import io.swagger.v3.oas.models.responses.{ApiResponse, ApiResponses}
import io.swagger.v3.oas.models.servers.{Server, ServerVariable, ServerVariables}
import io.swagger.v3.oas.models.{Components, OpenAPI, Operation, PathItem, Paths}
import modux.core.api.ModuleX
import modux.model.dsl.{CookieKind, HeaderKind, NameSpacedEntry, ParamDescriptor, RestEntry}
import modux.model.exporter.{MediaTypeDescriptor, SchemaDescriptor}
import modux.model.rest.RestProxy
import modux.model.schema.MSchema

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

object Exporter {

  def processor(buildContext: BuildContext): String = {

    def genContext(buildContext: BuildContext): Context = {
      val appClassloader: ClassLoader = buildContext.appClassloader
      val localConfig: Config = ConfigFactory.load(appClassloader)

      new Context {

        private val classic: Classic = Classic(buildContext.get("appName"), BootstrapSetup(Option(appClassloader), Option(localConfig), None))
        override val applicationLoader: ClassLoader = appClassloader
        override val applicationName: String = buildContext.settings.get("appName")
        override val actorSystem: ActorSystem[Nothing] = classic.toTyped
        override val config: Config = localConfig
        override val executionContext: ExecutionContext = ExecutionContext.Implicits.global
      }
    }

    def extractModules(buildContext: BuildContext, context: Context): Seq[ModuleX] = {

      context
        .config
        .get[List[String]]("modux.modules")
        .map(x => buildContext.appClassloader.loadClass(x))
        .map(c => c.getConstructor(classOf[Context]).newInstance(context).asInstanceOf[ModuleX])
    }

    def processModule(moduleX: ModuleX, paths: Paths, components: Components): Paths = {

      import scala.collection.JavaConverters._

      def add(components: Components, map: Map[String, MSchema]): Unit = {
        map.foreach { case (k, v) =>
          val value: Schema[_] = VersionAdapter3(v)
          components.addSchemas(k, value)
        }
      }

      moduleX
        .providers
        .flatMap { srv =>

          srv
            .serviceDef
            .serviceEntries
            .collect {
              case x: RestEntry => x
              case x: NameSpacedEntry => x
            }
            .flatMap {
              case x: NameSpacedEntry =>
                x
                  .restEntry
                  .collect { case y: RestEntry => (y, y.restService) }
                  .collect { case (z, y: RestProxy) => (z, y.copy(x.ns + y.path)) }

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
                path,
                xs.foldLeft(new PathItem) { case (acc, (entry, proxy)) =>

                  val operation: Operation = new Operation
                  entry._description.foreach(operation.setDescription)

                  //************** parameters **************//

                  val paramDesMap: Map[String, ParamDescriptor] = entry._paramDescriptor.map { x => x._kind.name -> x }.toMap
                  val cookieAndHeaderParams: Seq[Parameter] = entry._paramDescriptor.map(_._kind).collect {
                    case x: CookieKind =>
                      val cookieParam: CookieParameter = new CookieParameter
                      cookieParam.setName(x.name)
                      cookieParam.setSchema(new StringSchema)
                      cookieParam
                    case x: HeaderKind =>
                      val headerParam: HeaderParameter = new HeaderParameter
                      headerParam.setSchema(new StringSchema)
                      headerParam.setName(x.name)
                      headerParam
                  }

                  val params: Seq[Parameter] = (VersionAdapter3(proxy.pathParameter) ++ VersionAdapter3(proxy.queryParameter) ++ cookieAndHeaderParams)
                    .map { p =>
                      paramDesMap.get(p.getName).fold(p) { x =>
                        x._examples.foreach { case (k, v) =>
                          val example: Example = new Example
                          example.setValue(v)
                          p.addExample(k, example)
                        }

                        p.description(x._description.orNull)
                      }
                    }

                  val ls: java.util.List[Parameter] = new java.util.ArrayList[Parameter]()
                  ls.addAll(params.asJavaCollection)

                  if (!ls.isEmpty) {
                    operation.setParameters(ls)
                  }

                  //************** request body **************//

                  entry._expect.foreach { requestDescriptor =>

                    val content: Content = new Content
                    val requestBody: RequestBody = new RequestBody

                    val mt: MediaTypeDescriptor = requestDescriptor.mediaType
                    val schemaDescriptor: SchemaDescriptor = mt.schemaDescriptor
                    val s: Schema[_] = VersionAdapter3(schemaDescriptor.reference)

                    mt.mediaTypes.foreach { x =>
                      val mediaType: MediaType = new MediaType
                      mediaType.schema(s)
                      content.addMediaType(x, mediaType)
                    }

                    requestBody.setContent(content)
                    operation.setRequestBody(requestBody)
                    add(components, schemaDescriptor.references)
                  }

                  //************** response **************//
                  val apiResponses: ApiResponses = new ApiResponses
                  operation.setResponses(apiResponses)

                  entry._response.foreach { x =>
                    val apiResponse: ApiResponse = new ApiResponse
                    apiResponse.setDescription(x.description)
                    val examplesMap: Map[String, String] = x._example.asMap

                    x.schema.foreach { media =>
                      val content: Content = new Content

                      media.mediaTypes.foreach { z =>
                        val mt: MediaType = new MediaType
                        //                        mt.setSchema(VersionAdapter(media.schemaDescriptor.reference))
                        mt.setSchema(proxy.responseWith.map(z => VersionAdapter3(z.reference)).getOrElse(VersionAdapter3(media.schemaDescriptor.reference)))
                        examplesMap.get(z).foreach(mt.setExample(_))
                        content.addMediaType(z, mt)
                      }

                      add(components, media.schemaDescriptor.references)
                      apiResponse.setContent(content)
                    }

                    apiResponses.addApiResponse(x.code.toString, apiResponse)
                  }

                  add(components, proxy.schemas)

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
        }
        .foldLeft(paths) { case (acc, (path, pathItem)) =>
          acc.addPathItem(path, pathItem)
        }

    }

    def createInfo(buildContext: BuildContext): Info = {
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

      info
    }

    def processServers(buildContext: BuildContext): Seq[Server] = {

      buildContext.servers.asScala.map { x =>

        val server: Server = new Server
        val serverVariables: ServerVariables = new ServerVariables

        server.setUrl(x.url)
        server.setDescription(x.description)

        x.variables.asScala.foreach { case (k, v) =>

          val serverVar: ServerVariable = new ServerVariable
          serverVar.setDefault(v.default)
          serverVar.setEnum(v.values)
          serverVariables.addServerVariable(k, serverVar)
        }

        server.variables(serverVariables).description(x.description)
      }
    }

    def main(buildContext: BuildContext, context: Context): String = {
      import scala.collection.JavaConverters._

      val readerConfig: SwaggerConfiguration = new SwaggerConfiguration
      val openAPI: OpenAPI = new OpenAPI
      val components: Components = new Components
      val paths: Paths = extractModules(buildContext, context).foldLeft(new Paths) { case (acc, mod) => processModule(mod, acc, components) }

      openAPI
        .info(createInfo(buildContext))
        .paths(paths)
        .components(components)
        .servers(processServers(buildContext).asJava)

      val reader: Reader = new Reader(readerConfig.openAPI(openAPI))

      val set: Set[Class[_]] = Set()
      val swagger: OpenAPI = reader.read(set.asJava)

      buildContext.get("export.mode") match {
        case "json" =>
          Json.pretty(swagger)
        case _ =>
          Yaml.pretty(swagger)
      }
    }

    val context: Context = genContext(buildContext)
    val result: String = main(buildContext, context)
    context.actorSystem.terminate()
    result
  }
}

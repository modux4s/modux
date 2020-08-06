package modux.macros.service

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import modux.macros.MacroUtils._
import modux.macros.utils.SchemaUtils
import modux.model._
import modux.model.exporter.SchemaDescriptor
import modux.model.ws.WSEvent

import scala.collection.mutable
import scala.reflect.macros.blackbox
import scala.util.{Failure, Success, Try => stry}

//noinspection NotImplementedCode,DuplicatedCode
object ServiceSupportMacro {

  //************** ***** **************//


  def staticServe(c: blackbox.Context)(url: c.Expr[String], dir: c.Expr[String]): c.Expr[RestService] = {

    val staticUrl: String = {
      val x: String = c.eval(url)
      if (x.endsWith("/")) {
        x.substring(0, x.length - 1)
      } else {
        x
      }
    }
    val staticDir: String = c.eval(dir)

    val rule: String = staticUrl.split("/").map(x => x.qt).mkString("/")
    val slash: String = if (staticDir.endsWith("/")) "" else "/"

    c.Expr[RestService](
      c.parse(
        s"""
           |new modux.model.RestInstance {
           |  import akka.http.scaladsl.server.Directives.{get => akkaGet, path=>akkaPath}
           |  import akka.http.scaladsl.server.Directives._
           |  import akka.http.scaladsl.server.Route
           |
           |  override def route: Route = {
           |    (akkaGet & pathPrefix($rule)) {
           |      pathEndOrSingleSlash {
           |        getFromDirectory(s"$staticDir${slash}index.html")
           |      } ~
           |      getFromDirectory("$staticDir")
           |    }
           |  }
           |}
           |""".stripMargin
      )
    )
  }

  def callRestBuilder(c: blackbox.Context)(checkName: Boolean, url: c.Expr[String], funcRef: c.Tree): c.Expr[RestService] = {
    import c.universe._

    val callType: c.universe.Type = funcRef.tpe.resultType.typeArgs.last
    val requestType: c.universe.Type = callType.typeArgs.head
    val isEmptyRequest: Boolean = same(c)(requestType, typeOf[NotUsed], definitions.UnitTpe)

    val nameCond: Boolean = {
      !checkName || {
        stry(c.eval(url)) match {
          case Failure(_) =>
            c.abort(c.enclosingPosition, "You must pass a name as constant")
          case Success(value) =>
            value.matches(nameRegex)
        }
      }
    }

    if (!nameCond) {
      c.abort(c.enclosingPosition, "Invalid name. It must match with '[a-z-A-Z][a-z-A-Z0-9]*'")
    } else {
      if (isEmptyRequest) {
        restServiceBuilder(c)("get", url, funcRef)
      } else {
        restServiceBuilder(c)("post", url, funcRef)
      }
    }
  }

  def restServiceBuilder(c: blackbox.Context)(method: String, url: c.Expr[String], funcRef: c.Tree): c.Expr[RestService] = {
    import c.universe._

    //************** INITIALIZATION **************//
    val argsMap: Map[String, c.universe.ValDef] = funcRef.collect { case t: ValDef => t }.map(x => x.name.toString -> x).toMap
    val isCompile: Boolean = sys.props.get("modux.mode").forall(_ == "compile")
    lazy val urlValue: String = c.eval[String](url)
    lazy val parsedPath: PathMetadata = extractVariableName(c)(urlValue)
    lazy val parsedArgumentsMap: Map[String, AsPathParam] = parsedPath.parsedArgumentsMap

    if (parsedArgumentsMap.size + parsedPath.queryParams.size != argsMap.size) {
      c.abort(c.enclosingPosition, s"Arguments lengths doesn't match with path arguments length in path '$urlValue'.")
    } else if (!argsMap.keySet.forall(x => parsedPath.queryParams.contains(x) || parsedArgumentsMap.contains(x))) {
      c.abort(c.enclosingPosition, s"Some arguments name doesn't match with the path arguments.")
    } else if (parsedPath.queryParams.isEmpty && parsedPath.pathParams.isEmpty) {
      c.abort(c.enclosingPosition, "Invalid path")
    }

    val treeBuild: String = {
      if (isCompile) {
        serverMode(c)(method, urlValue, funcRef, parsedPath)
      } else {
        exportMode(c)(method, urlValue, funcRef, parsedPath)
      }
    }

    c.Expr(c.parse(treeBuild))
  }

  def extractSchemaMacro[A: c.WeakTypeTag](c: blackbox.Context): c.Expr[Option[SchemaDescriptor]] = {
    import c.universe._
    val schemaUtil: SchemaUtils[c.type] = new SchemaUtils(c)
    val str: String = schemaUtil.extractSchema(weakTypeOf[A])
    c.Expr[Option[SchemaDescriptor]](c.parse(str))
  }

  def serverMode(c: blackbox.Context)(method: String, urlValue: String, funcRef: c.Tree, parsedPath: PathMetadata): String = {
    import c.universe._

    //************** UTILS **************//
    def inferMethod(defaultValue: String, hasNotUsedParam: Boolean): String = {
      if (hasNotUsedParam) s"akka.http.scaladsl.server.Directives.$defaultValue"
      else "akka.http.scaladsl.server.Directives.post"
    }

    def getType(tag: c.universe.Type): String = tag.toString match {
      case "Int" => "IntNumber"
      case "String" => "Segment"
      case "Double" => "DoubleNumber"
      case _ => c.abort(c.enclosingPosition, s"Supported type for path params: Int, String and Double. Received $tag.")
    }

    def iterator(params: Seq[Path], argsMap: Map[String, c.universe.ValDef]): Seq[String] = params match {
      case Nil => Nil
      case x +: xs =>

        x match {
          case AsPath(name) => s""" "$name" """ +: iterator(xs, argsMap)
          case AsPathParam(name) =>

            argsMap.get(name) match {
              case Some(value) => getType(value.tpt.tpe) +: iterator(xs, argsMap)
              case None => c.abort(c.enclosingPosition, s"Missing argument $name")
            }
        }
    }

    def pathBuilder(params: Seq[Path], argsMap: Map[String, c.universe.ValDef]): String = iterator(params, argsMap).mkString(" / ")

    def getConverter(t: c.Type): String = {
      val str: String = t.toString
      if (SUPPORTED_ITERABLE.contains(str)) {
        s".to$str"
      } else {
        c.abort(c.enclosingPosition, s"Unsupported iterable $str. Supported ${SUPPORTED_ITERABLE.mkString(", ")}.")
      }
    }

    //************** INITIALIZATION **************//
    val callType: c.universe.Type = funcRef.tpe.resultType.typeArgs.last
    val argsName: Seq[String] = funcRef.collect { case t: ValDef => t }.map(_.name.toString)
    val argsMap: Map[String, c.universe.ValDef] = funcRef.collect { case t: ValDef => t }.map(x => x.name.toString -> x).toMap

    val requestType: c.universe.Type = callType.typeArgs.head
    val responseType: c.universe.Type = callType.typeArgs.last

    val hasNotUsedParam: Boolean = same(c)(requestType, typeOf[NotUsed], definitions.UnitTpe)
    val isEmptyRequest: Boolean = same(c)(requestType, typeOf[NotUsed], definitions.UnitTpe)
    val isEmptyResponse: Boolean = same(c)(responseType, typeOf[NotUsed], definitions.UnitTpe)
    val isWebSocket: Boolean = same(c)(requestType, typeOf[WSEvent[_, _]])
    val isSourceRequest: Boolean = same(c)(requestType, typeOf[Source[_, _]])
    //    val isSourceResponse: Boolean = same(c)(responseType, typeOf[Source[_, _]])

    lazy val pathTpl: String = {
      val tmp: String = pathBuilder(parsedPath.pathParams, argsMap)
      if (isWebSocket) {
        s"(__path__($tmp))"
      } else {
        s"(__path__($tmp) & ${inferMethod(method, hasNotUsedParam)})"
      }
    }

    lazy val errorTpl: String =
      s"""
         |e match {
         |  case m:ResponseAsFalseFail => complete(m.data)
         |  case x => failWith(x)
         |}
         |""".stripMargin

    lazy val responseTpl: String = {

      if (isEmptyResponse) {
        "complete(StatusCodes.OK)"
      } else {
        "complete(__value__)"
      }
    }

    lazy val entityTpl: String = {

      if (isWebSocket) {
        s"""
           |extractRequest{ __request__ =>
           |  handleWebSocketMessages(webSocketManager.listen(AkkaUtils(__request__)))
           |}
           |""".stripMargin
      } else if (isSourceRequest) {

        val isByteString: Boolean = isSourceRequest && requestType.typeArgs.headOption.exists(x => same(c)(x, typeOf[ByteString]))

        if (isByteString) {
          s"""
             |extractDataBytes{__src__ =>
             |  extractRequest{__request__ =>
             |    val srv = serviceCall.transform(__src__, AkkaUtils(__request__))
             |    onComplete(srv){
             |      case Failure(e) =>
             |        $errorTpl
             |      case Success((__value__, __requestHeader__)) => AkkaUtils.render(__requestHeader__){$responseTpl}
             |    }
             |  }
             |}
             |""".stripMargin
        } else {
          val requestTypeStr: String = fullName(c)(requestType.typeArgs.head)
          s"""
             |extractRequest{__request__ =>
             |  import modux.macros.serializer.SerializationSupport
             |
             |  entity(SerializationSupport.moduxAsSource[$requestTypeStr]){__src__ =>
             |    val srv = serviceCall.transform(__src__, AkkaUtils(__request__))
             |    onComplete(srv){
             |      case Failure(e) =>
             |        $errorTpl
             |      case Success((__value__, __requestHeader__)) => AkkaUtils.render(__requestHeader__){$responseTpl}
             |    }
             |  }
             |}
             |""".stripMargin
        }
      } else {
        val requestTypeStr: String = fullName(c)(requestType)

        if (isEmptyRequest) {
          s"""
             |extractRequest{__request__ =>
             |  val srv = serviceCall.transform($requestTypeStr, AkkaUtils(__request__))
             |  onComplete(srv){
             |    case Failure(e) =>
             |      $errorTpl
             |    case Success((__value__, __requestHeader__)) => AkkaUtils.render(__requestHeader__){$responseTpl}
             |  }
             |}
             |"""
        } else {
          s"""
             |extractRequest{ __request__ =>
             |  entity(as[$requestTypeStr]){ __entity__ =>
             |    val srv = serviceCall.transform(__entity__, AkkaUtils(__request__))
             |    onComplete(srv){
             |      case Failure(e) =>
             |        $errorTpl
             |      case Success((__value__, __requestHeader__)) => AkkaUtils.render(__requestHeader__){$responseTpl}
             |    }
             |  }
             |}
             |""".stripMargin
        }
      }
    }

    lazy val extraAttrs: String = {
      if (isWebSocket) {
        val inType: String = requestType.typeArgs.mkString(",")
        val wsType: String = s"modux.core.ws.WebSocketManager[$inType]"
        s"""private val webSocketManager: $wsType = $wsType("$urlValue",callback())"""
      } else {
        ""
      }
    }

    lazy val bodyTpl: String = {
      val parsedArguments: Seq[AsPathParam] = parsedPath.parsedArguments
      val argsType: String = parsedArguments.flatMap(x => argsMap.get(x.name)).map(x => s""" ${x.name}: ${x.tpt.tpe} """).mkString("(", ",", ") =>")
      val pathArguments: String = if (parsedArguments.isEmpty) "" else argsType
      val argumentsCall: String = argsName.mkString("(", ", ", ")")

      //************** query params processing **************//

      val returnTpl: String = {
        if (isWebSocket) {
          entityTpl
        } else {
          s"""
             |val serviceCall: ${callType.toString} = callback$argumentsCall
             |$entityTpl
             |""".stripMargin
        }
      }

      val queryParamTpl: String = {
        if (parsedPath.queryParams.isEmpty) {
          returnTpl
        } else {
          // (name - arg name - parameter arg - converter)
          val queryParams: Seq[(String, String, String, String)] = parsedPath
            .queryParams
            .flatMap(x => argsMap.get(x))
            .map { met =>

              val name: String = met.name.toString
              val tpe: c.Type = met.tpt.tpe
              val argName: String = s"${name}Tmp"

              val (param, converter) = {
                if (tpe =:= typeOf[String]) {
                  if (tpe.weak_<:<(typeOf[Option[_]])) {
                    (s""" "$name".? """, "")
                  } else if (tpe.weak_<:<(typeOf[Iterable[_]])) {
                    (s""" "$name".* """, getConverter(tpe.typeConstructor))
                  } else {
                    (s""" "$name" """, "")
                  }
                } else {
                  if (tpe.weak_<:<(typeOf[Option[_]])) {
                    val tpeStr: String = tpe.typeArgs.mkString(",")
                    (s""" "$name".as[$tpeStr].? """, "")
                  } else if (tpe.weak_<:<(typeOf[Iterable[_]])) {
                    val tpeStr: String = tpe.typeArgs.mkString(",")
                    (s""" "$name".as[$tpeStr].* """, getConverter(tpe.typeConstructor))
                  } else {
                    (s""" "$name".as[$tpe] """, "")
                  }
                }
              }
              (name, argName, param, converter)
            }

          val parameterArgs: String = queryParams.map { case (_, _, x, _) => x }.mkString(", ")
          val callbackArgs: String = queryParams.map { case (m, n, _, x) => if (x.isEmpty) m else n }.mkString(", ")
          val bodyVal: String = queryParams
            .filterNot { case (_, _, _, x) => x.isEmpty }
            .map { case (nameVal, argName, _, converter) =>
              s"val $nameVal = $argName$converter"
            }.mkString("\n")

          s"""
             |parameters($parameterArgs){ ($callbackArgs) =>
             |  $bodyVal
             |  $returnTpl
             |}
             |""".stripMargin
        }
      }

      //************** paths params processing **************//

      s"""
         |{$pathArguments
         |  $queryParamTpl
         |}
         |""".stripMargin
    }

    s"""
       |new modux.model.RestInstance {
       |    import akka.http.scaladsl.server.Directives._
       |    import scala.util.{Success, Failure}
       |    import akka.http.scaladsl.server.Directives.{path => __path__}
       |    import modux.model._
       |    import akka.http.scaladsl.server.Route
       |    import akka.http.scaladsl.model.StatusCodes
       |    import org.slf4j.LoggerFactory
       |    import modux.model.header._
       |    import akka.http.scaladsl.common.EntityStreamingSupport
       |    import modux.macros.utils.AkkaUtils
       |    import modux.model.dsl.ResponseAsFalseFail
       |
       |    private val __logger__ = LoggerFactory.getLogger(this.getClass())
       |    private val routeImpl: Route = $pathTpl($bodyTpl)
       |    private val callback = $funcRef
       |    $extraAttrs
       |
       |    override def route: Route = routeImpl
       |}
       |""".stripMargin
  }

  def exportMode(c: blackbox.Context)(method: String, urlValue: String, funcRef: c.Tree, parsedPath: PathMetadata): String = {
    import c.universe._
    val schemaUtil: SchemaUtils[c.type] = new SchemaUtils(c)

    //************** INITIALIZATION **************//
    val callType: c.universe.Type = funcRef.tpe.resultType.typeArgs.last
    val argsMap: Map[String, c.universe.ValDef] = funcRef.collect { case t: ValDef => t }.map(x => x.name.toString -> x).toMap

    val requestType: c.universe.Type = callType.typeArgs.head
    val responseType: c.universe.Type = callType.typeArgs.last

    val isEmptyRequest: Boolean = same(c)(requestType, typeOf[NotUsed], definitions.UnitTpe)
    val isEmptyResponse: Boolean = same(c)(responseType, typeOf[NotUsed], definitions.UnitTpe)
    val isWebSocket: Boolean = same(c)(requestType, typeOf[WSEvent[_, _]])
    val isSourceRequest: Boolean = same(c)(requestType, typeOf[Source[_, _]])
    val isSourceResponse: Boolean = same(c)(responseType, typeOf[Source[_, _]])

    val queryParamInf: String = parsedPath.queryParams.flatMap(x => argsMap.get(x)).map { x =>

      val name: String = x.name.toString
      val tpe: c.universe.Type = x.tpt.tpe

      val schema: String = schemaUtil.primitiveSchema(tpe, Map.empty).fold("")(x => s"qp.setSchema($x)")

      s"""
         |{
         |  val qp:QueryParameter = new QueryParameter
         |  qp.setName("$name")
         |  $schema
         |  qp
         |}
         |""".stripMargin
    }.mkString("Seq(", ",", ")")

    val pathParamInf: String = parsedPath.pathParams.collect { case x: AsPathParam => x }.flatMap(x => argsMap.get(x.name)).map { x =>
      val name: String = x.name.toString
      val tpe: c.universe.Type = x.tpt.tpe

      val schema: String = schemaUtil.primitiveSchema(tpe, Map.empty).fold("")(x => s"pp.setSchema($x)")
      s"""
         |{
         |  val pp:PathParameter = new PathParameter
         |  pp.setName("$name")
         |  $schema
         |  pp
         |}
         |""".stripMargin
    }.mkString("Seq(", ",", ")")

    val storeRef: mutable.Map[String, String] = mutable.Map.empty

    val requestMat: String = {
      if (isEmptyRequest || isSourceRequest || isWebSocket)
        "None"
      else {
        val _ = schemaUtil.iterator(requestType, storeRef, Map.empty)
        schemaUtil.extractSchema(requestType, storeRef)
      }
    }

    val responseMat: String = {
      if (isEmptyResponse || isSourceResponse || isWebSocket) {
        "None"
      } else {
        val _ = schemaUtil.iterator(responseType, storeRef, Map.empty)
        schemaUtil.extractSchema(responseType, storeRef)
      }
    }

    val joinedSchemas: String = schemaUtil.joiner(storeRef.toMap)

    s"""
       |new modux.model.RestProxy {
       |
       |  import io.swagger.v3.oas.models.media._
       |  import io.swagger.v3.oas.models.parameters._
       |  import modux.model.exporter.SchemaDescriptor
       |
       |  override def ignore: Boolean = $isWebSocket
       |  override def pathParameter: Seq[Parameter] = $pathParamInf
       |  override def queryParameter: Seq[Parameter] = $queryParamInf
       |  override def schemas: Map[String, Schema[_]] = $joinedSchemas
       |  override def path: String = "${normalizePath(urlValue)}"
       |  override def method: String = "$method"
       |  override def requestWith: Option[SchemaDescriptor] = $requestMat
       |  override def responseWith: Option[SchemaDescriptor] = $responseMat
       |}
       |""".stripMargin
  }
}

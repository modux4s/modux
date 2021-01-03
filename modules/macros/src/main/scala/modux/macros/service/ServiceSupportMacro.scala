package modux.macros.service

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import modux.macros.MacroUtils
import modux.macros.MacroUtils._
import modux.macros.utils.SchemaUtils
import modux.model.dsl.RestEntry
import modux.model.exporter.SchemaDescriptor
import modux.model.rest._
import modux.model.ws.WSEvent

import scala.collection.mutable
import scala.reflect.macros.blackbox
import scala.util.{Failure, Success, Try => stry}

//noinspection NotImplementedCode,DuplicatedCode
object ServiceSupportMacro {

  def staticServe(c: blackbox.Context)(url: c.Expr[String], dir: c.Expr[String]): c.Expr[RestEntry] = {

    val staticUrl: String = {
      val x: String = c.eval(url)
      val v1: String = if (x.endsWith("/")) {
        x.substring(0, x.length - 1)
      } else {
        x
      }

      if (v1.startsWith("/")) v1.substring(1) else v1
    }
    val staticDir: String = c.eval(dir)

    val rule: String = staticUrl.split("/").map(x => x.qt).mkString("/")
    val slash: String = if (staticDir.endsWith("/")) "" else "/"

    c.Expr[RestEntry](
      c.parse(
        s"""
           |modux.model.dsl.RestEntry(
           |  new modux.model.rest.RestInstance {
           |    import akka.http.scaladsl.server.Directives.{get => akkaGet, path=>akkaPath}
           |    import akka.http.scaladsl.server.Directives._
           |    import akka.http.scaladsl.server.Route
           |    import modux.model.dsl.RestEntryExtension
           |
           |    override def route(extensions: Seq[RestEntryExtension]): Route = {
           |      (akkaGet & pathPrefix($rule)) {
           |        pathEndOrSingleSlash {
           |          getFromDirectory(s"$staticDir${slash}index.html")
           |        } ~
           |        getFromDirectory("$staticDir")
           |      }
           |    }
           |  }
           |)
           |""".stripMargin
      )
    )
  }

  def callRestBuilder(c: blackbox.Context)(checkName: Boolean, url: c.Expr[String], funcRef: c.Tree): c.Expr[RestEntry] = {
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

  def restServiceBuilder(c: blackbox.Context)(method: Option[String], url: c.Expr[String], funcRef: c.Tree): c.Expr[RestEntry] = {
    import c.universe._

    //************** INITIALIZATION **************//
    val argsMap: Map[String, c.universe.ValDef] = funcRef.collect { case t: ValDef => t }.map(x => x.name.toString -> x).toMap

    val isCompile: Boolean = sys.props.get("modux.mode").forall(_ == "compile")
    lazy val urlValue: String = c.eval[String](url)
    lazy val parsedPath: PathMetadata = extractVariableName(c)(urlValue)
    lazy val parsedArgumentsMap: Map[String, Path] = parsedPath.parsedArgumentsMap

    val parsedArgMapSize: Int = parsedArgumentsMap.size
    val expectedSize: Int = parsedArgMapSize + parsedPath.queryParams.size
    val functionRefArgsSize: Int = if (funcRef.tpe.typeArgs.head.toString.contains("Call")) 0 else funcRef.tpe.typeArgs.dropRight(1).size
    val matchByTypes: Boolean = functionRefArgsSize == parsedArgMapSize

    if (expectedSize != functionRefArgsSize) {
      c.abort(c.enclosingPosition, s"Arguments lengths doesn't match with path arguments length in path '$urlValue'.")
    } else {
      val matchByNameArgs: Boolean = argsMap.keySet.forall(x => parsedPath.queryParams.contains(x) || parsedArgumentsMap.contains(x))

      if (!(argsMap.size == 1 && parsedPath.hasAnything && parsedArgMapSize == 1) && !(matchByNameArgs || matchByTypes)) {
        c.abort(c.enclosingPosition, s"Some arguments name doesn't match with the path arguments.")
      } else if (parsedPath.queryParams.isEmpty && parsedPath.pathParams.isEmpty) {
        c.abort(c.enclosingPosition, "Invalid path")
      }
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

  def serverMode(c: blackbox.Context)(method: Option[String], urlValue: String, funcRef: c.Tree, parsedPath: PathMetadata): String = {
    import c.universe._

    //************** UTILS **************//
    def inferMethod(defaultValue: Option[String], hasNotUsedParam: Boolean): String = {
      defaultValue match {
        case Some(v) =>
          val result: String = {
            if (hasNotUsedParam) {
              s"akka.http.scaladsl.server.Directives.$v"
            } else {
              "akka.http.scaladsl.server.Directives.post"
            }
          }

          s" & $result"
        case None => ""
      }
    }

    def getType(tag: String): String = tag match {
      case "Int" => "IntNumber"
      case "String" => "Segment"
      case "Double" => "DoubleNumber"
      case _ => c.abort(c.enclosingPosition, s"Supported type for path params: Int, String and Double. Received $tag.")
    }

    def pathBuilder(params: Seq[Path], argsMap: Map[String, String]): String = {

      def iterator(params: Seq[Path], argsMap: Map[String, String]): Seq[String] = params match {
        case Nil => Nil
        case x +: xs =>

          //noinspection ScalaUnusedSymbol
          x match {
            case AsPath(name) => s""" "$name" """ +: iterator(xs, argsMap)
            case AsPathParam(name) =>

              argsMap.get(name) match {
                case Some(value) => getType(value) +: iterator(xs, argsMap)
                case None => c.abort(c.enclosingPosition, s"Missing argument $name")
              }
            case AnythingPath => Seq("  Remaining ")
            case AsRegexPath(name, regex) => c.abort(c.enclosingPosition, s"Missing implementation of RegexPath")
          }
      }

      iterator(params, argsMap).mkString(" / ")
    }

    def getConverter(str: String): String = {
      if (SUPPORTED_ITERABLE.contains(str)) {
        s".to$str"
      } else {
        c.abort(c.enclosingPosition, s"Unsupported iterable $str. Supported ${SUPPORTED_ITERABLE.mkString(", ")}.")
      }
    }

    //************** INITIALIZATION **************//

    val hasNoArgs: Boolean = funcRef.tpe.typeConstructor.toString.startsWith("modux.model.service.Call")
    val callType: c.universe.Type = {
      if (hasNoArgs)
        funcRef.tpe.resultType
      else
        funcRef.tpe.resultType.typeArgs.last
    }

    val (argsName, argsMap) = {
      val args: List[c.universe.Type] = funcRef.tpe.typeArgs
      val finalArgs: List[c.universe.Type] = if (args.size > 1) args.dropRight(1) else Nil

      val argsNameLocal: Seq[String] = parsedPath.pathParams.collect {
        case AsPathParam(name) => name
        case AnythingPath => "rem"
      } ++ parsedPath.queryParams

      val argsTypesLocal: List[String] = finalArgs.map(_.toString)
      val argsMap: Map[String, String] = argsNameLocal.zip(argsTypesLocal).toMap

      (argsNameLocal, argsMap)

    }

    val requestType: c.universe.Type = callType.typeArgs.head
    val responseType: c.universe.Type = callType.typeArgs.last

    val hasNotUsedParam: Boolean = same(c)(requestType, typeOf[NotUsed], definitions.UnitTpe)
    val isEmptyRequest: Boolean = same(c)(requestType, typeOf[NotUsed], definitions.UnitTpe)
    val isEmptyResponse: Boolean = same(c)(responseType, typeOf[NotUsed], definitions.UnitTpe)
    val isWebSocket: Boolean = same(c)(requestType, typeOf[WSEvent[_, _]])
    val isSourceRequest: Boolean = same(c)(requestType, typeOf[Source[_, _]])

    lazy val pathTpl: String = {
      val tmp: String = pathBuilder(parsedPath.pathParams, argsMap)

      if (isWebSocket) {
        s"(__path__($tmp))"
      } else {
        s"(__path__($tmp) ${inferMethod(method, hasNotUsedParam)})"
      }
    }

    lazy val errorTpl: String =
      s"""
         |e match {
         |  case m: ResponseAsFalseFail => complete(m.data)
         |  case ex: CircuitBreakerOpenException =>
         |    extractRequestContext { _ctx_ =>
         |      _ctx_.request.entity.dataBytes.runWith(Sink.cancelled)(_ctx_.materializer)
         |      reject(CircuitBreakerOpenRejection(ex))
         |    }
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

    def entityTpl: String = {

      val onCompleteTpl: String = "__extensions.foldLeft(srv) { case (acc, x) => x.call(acc) }"

      if (isWebSocket) {
        s"""
           |extractRequestContext{ __request__ =>
           |  handleWebSocketMessages(webSocketManager.listen(__request__))
           |}
           |""".stripMargin
      } else if (isSourceRequest) {

        val isByteString: Boolean = isSourceRequest && requestType.typeArgs.headOption.exists(x => same(c)(x, typeOf[ByteString]))

        if (isByteString) {
          s"""
             |extractDataBytes{__src__ =>
             |  extractRequestContext{__request__ =>
             |    val __invoke__ = AkkaUtils.createInvoke(__request__)
             |    val srv = AkkaUtils.check(serviceCall(__src__, __invoke__))
             |    onComplete($onCompleteTpl){
             |      case Failure(e) =>
             |        $errorTpl
             |      case Success(__value__) => AkkaUtils.mapResponse(__invoke__){$responseTpl}
             |    }
             |  }
             |}
             |""".stripMargin
        } else {
          val requestTypeStr: String = fullName(c)(requestType.typeArgs.head)
          s"""
             |extractRequestContext{__request__ =>
             |  import modux.macros.utils.SerializationUtil
             |
             |  entityAkka(SerializationUtil.moduxAsSource[$requestTypeStr]){__src__ =>
             |    val __invoke__ = AkkaUtils.createInvoke(__request__)
             |    val srv = AkkaUtils.check(serviceCall(__src__, __invoke__))
             |    onComplete($onCompleteTpl){
             |      case Failure(e) =>
             |        $errorTpl
             |      case Success(__value__) => AkkaUtils.mapResponse(__invoke__){$responseTpl}
             |    }
             |  }
             |}
             |""".stripMargin
        }
      } else {

        if (isEmptyRequest) {
          val requestTypeStr: String = if (same(c)(requestType, definitions.UnitTpe)) "()" else fullName(c)(requestType)
          s"""
             |extractRequestContext{__request__ =>
             |  val __invoke__ = AkkaUtils.createInvoke(__request__)
             |  val srv = AkkaUtils.check(serviceCall($requestTypeStr, __invoke__))
             |  onComplete($onCompleteTpl){
             |    case Failure(e) =>
             |      $errorTpl
             |    case Success(__value__) => AkkaUtils.mapResponse(__invoke__){$responseTpl}
             |  }
             |}
             |"""
        } else {
          s"""
             |extractRequestContext{ __request__ =>
             |  entityAkka(as[$requestType]){ __entity__ =>
             |    val __invoke__ = AkkaUtils.createInvoke(__request__)
             |    val srv = AkkaUtils.check(serviceCall(__entity__, __invoke__))
             |    onComplete($onCompleteTpl){
             |      case Failure(e) =>
             |        $errorTpl
             |      case Success(__value__) => AkkaUtils.mapResponse(__invoke__){$responseTpl}
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

    def bodyTpl: String = {

      //val parsedArgumentsMap: Map[String, Path] = parsedPath.parsedArgumentsMap
      val parsedArguments: Seq[Path] = parsedPath.parsedArguments

      val additionalArguments: Seq[String] = {
        if (parsedPath.hasAnything) {
          Seq(s" rem: String")
        } else {
          Nil
        }
      }

      val argsType: String = (
        parsedArguments
          .flatMap { x =>
            val name: String = x.name
            for (v <- argsMap.get(name)) yield s""" $name: $v """
          } ++ additionalArguments
        )
        .mkString("(", ",", ") =>")

      val pathArguments: String = if (parsedArguments.isEmpty) "" else argsType
      val argumentsCall: String = if (hasNoArgs) "" else argsName.mkString("(", ", ", ")")

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
            .flatMap { x =>
              for (met <- argsMap.get(x)) yield {

                c.echo(c.enclosingPosition, met)
                val name: String = x
                val tpe: String = met
                val argName: String = s"${name}Tmp"

                val (param, converter) = {
                  if (tpe == "String") {
                    if (tpe.startsWith("Option")) {
                      (s""" "$name".? """, "")
                    } else if (MacroUtils.isIterable(tpe)) {
                      (s""" "$name".* """, getConverter(MacroUtils.extractSuperType(tpe)))
                    } else {
                      (s""" "$name" """, "")
                    }
                  } else {
                    if (tpe.startsWith("Option")) {
                      val tpeStr: String = MacroUtils.extractSubType(tpe)
                      (s""" "$name".as[$tpeStr].? """, "")
                    } else if (MacroUtils.isIterable(tpe)) {
                      val tpeStr: String = MacroUtils.extractSubType(tpe)
                      (s""" "$name".as[$tpeStr].* """, getConverter(MacroUtils.extractSuperType(tpe)))
                    } else {
                      (s""" "$name".as[$tpe] """, "")
                    }
                  }
                }
                (name, argName, param, converter)
              }
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
         |    {$pathArguments
         |      $queryParamTpl
         |    }
         |""".stripMargin
    }

    val tpl: String = {

      s"""
         |ignoreTrailingSlash  {
         |  $pathTpl$bodyTpl
         |}
         |""".stripMargin
    }

    s"""
       |modux.model.dsl.RestEntry(
       |  new modux.model.rest.RestInstance {
       |      import akka.http.scaladsl.server.Directives._
       |      import scala.util.{Success, Failure, Try}
       |      import scala.concurrent.Future
       |      import akka.http.scaladsl.server.Directives.{path => __path__, entity => entityAkka}
       |      import modux.model._
       |      import akka.http.scaladsl.server.Route
       |      import akka.http.scaladsl.model.StatusCodes
       |      import modux.model.header._
       |      import akka.http.scaladsl.common.EntityStreamingSupport
       |      import modux.macros.utils.AkkaUtils
       |      import modux.model.dsl.ResponseAsFalseFail
       |      import akka.pattern.CircuitBreakerOpenException
       |      import akka.pattern.CircuitBreaker
       |      import akka.http.scaladsl.server.CircuitBreakerOpenRejection
       |      import akka.stream.scaladsl.Sink
       |      import modux.model.dsl.RestEntryExtension
       |
       |      private val callback = {
       |        $funcRef
       |      }
       |      $extraAttrs
       |
       |      override def route(__extensions: Seq[RestEntryExtension]): Route = {
       |        $tpl
       |      }
       |  }
       |  )
       |""".stripMargin
  }

  def exportMode(c: blackbox.Context)(method: Option[String], urlValue: String, funcRef: c.Tree, parsedPath: PathMetadata): String = {
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
      val schema: Option[String] = schemaUtil.primitiveSchema(tpe)
      s"""MParameter("$name", "query", true, ${schemaUtil.matOpt(schema)})"""
    }.mkString("Seq(", ",", ")")

    val pathParamInf: String = parsedPath.pathParams.collect { case x: AsPathParam => x }.flatMap(x => argsMap.get(x.name)).map { x =>
      val name: String = x.name.toString
      val tpe: c.universe.Type = x.tpt.tpe

      val schema: Option[String] = schemaUtil.primitiveSchema(tpe)
      s"""MParameter("$name", "path", true, ${schemaUtil.matOpt(schema)})"""
    }.mkString("Seq(", ",", ")")

    val storeRef: mutable.Map[String, String] = mutable.Map.empty

    val requestMat: String = {
      if (isEmptyRequest || isWebSocket)
        "None"
      else if (isSourceRequest) {
        val head: c.universe.Type = requestType.typeArgs.head
        schemaUtil.extractArraySchema(head, storeRef)
      } else {
        val _ = schemaUtil.iterator(requestType, storeRef, isRequired = false, isNullable = false)
        schemaUtil.extractSchema(requestType, storeRef)
      }
    }

    val responseMat: String = {
      if (isEmptyResponse || isWebSocket) {
        "None"
      } else if (isSourceResponse) {
        val head: c.universe.Type = responseType.typeArgs.head
        schemaUtil.extractArraySchema(head, storeRef)
      } else {
        val _ = schemaUtil.iterator(responseType, storeRef, isRequired = false, isNullable = false)
        schemaUtil.extractSchema(responseType, storeRef)
      }
    }

    val joinedSchemas: String = schemaUtil.joiner(storeRef.toMap)

    s"""
       |{
       |  import modux.model.exporter.SchemaDescriptor
       |  import modux.model.schema.{MParameter, MSchema, MRefSchema, MPrimitiveSchema, MArraySchema, MComposed}
       |  modux.model.dsl.RestEntry(
       |    modux.model.rest.RestProxy(
       |      ignore = $isWebSocket,
       |      pathParameter = $pathParamInf,
       |      queryParameter = $queryParamInf,
       |      schemas = $joinedSchemas,
       |      path = "${normalizePath(urlValue)}",
       |      method = "$method",
       |      requestWith = $requestMat,
       |      responseWith = $responseMat
       |    )
       |  )
       |}
       |""".stripMargin
  }
}

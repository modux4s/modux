package modux.macros.utils

import java.sql.{Time, Timestamp, Date => SQLDate}
import java.time._
import java.util.Date

import modux.macros.MacroUtils._

import scala.collection.mutable
import scala.reflect.macros.blackbox


case class Kind(primitive: Boolean, ref: Option[String], schema: Option[String]) {
  def one: Option[String] = ref.orElse(schema)
}

// https://users.scala-lang.org/t/how-to-use-data-structures-referencing-context-in-macros/3174
class SchemaUtils[C <: blackbox.Context with Singleton](val c: C) {

  import c.universe._


  def joiner(m: Map[String, String]): String = m.map { case (k, v) => s"${k.qt} -> $v" }.mkString("Map[String, Schema[_]](", ",", ")")

  def schemaDefiner(kind: String, xs: Seq[(String, Seq[String])]): String = {
    val calls: String = xs.map { case (met, args) => s"s.$met(${args.mkString(",")})" }.mkString("\n")

    val instancer: String = kind match {
      case "String" => "val s = new StringSchema"
      case "Boolean" => "val s = new BooleanSchema"
      case _ => s"val s:Schema[$kind] = new Schema()"
    }

    s"""
       |{
       |  $instancer
       |  $calls
       |  s
       |}
       |""".stripMargin
  }

  def schema(kind: String, tpe: String, xs: Seq[(String, Seq[String])] = Nil): String = schemaDefiner(kind, ("setType", Seq(tpe.qt)) +: xs)

  def buildFlags(m: Map[String, String]): Seq[(String, Seq[String])] = m.filter { case (_, v) => v == "true" }.map { case (k, v) => (k, Seq(v)) }.toSeq

  def callSchemaExtractor(tpe: c.universe.Type, store: mutable.Map[String, String] = mutable.Map.empty): Kind = iterator(tpe, store, Map.empty)

  def primitiveSchema(tpe: c.universe.Type, flags: Map[String, String]): Option[String] = {

    lazy val currentTime: Long = System.currentTimeMillis()

    if (tpe =:= typeOf[String]) {
      schema("String", "string", buildFlags(flags))
    } else if (tpe =:= typeOf[Int] || tpe =:= typeOf[Short]) {
      schema("Int", "integer", ("setFormat", Seq("int32".qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[Byte]) {
      schema("Byte", "byte", buildFlags(flags))
    } else if (tpe =:= typeOf[Long]) {
      schema("Long", "integer", ("setFormat", Seq("int64".qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[Float]) {
      schema("Float", "number", ("setFormat", Seq("float".qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[Double]) {
      schema("Double", "number", ("setFormat", Seq("double".qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[Boolean]) {
      schema("Boolean", "boolean", buildFlags(flags))
    } else if (tpe =:= typeOf[Date]) {
      schema("String", "string", ("setExample", Seq(new Date().toString.qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[SQLDate]) {
      schema("String", "string", ("setExample", Seq(new SQLDate(currentTime).toString.qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[Timestamp]) {
      schema("String", "string", ("setExample", Seq(new Timestamp(currentTime).toString.qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[Instant]) {
      schema("String", "string", ("setExample", Seq(Instant.now().toString.qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[LocalDateTime]) {
      schema("String", "string", ("setExample", Seq(LocalDateTime.now().toString.qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[LocalDate]) {
      schema("String", "string", ("setExample", Seq(LocalDate.now().toString.qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[Time]) {
      schema("String", "string", ("setExample", Seq(new Time(currentTime).toString.qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[LocalTime]) {
      schema("String", "string", ("setExample", Seq(LocalTime.now().toString.qt)) +: buildFlags(flags))
    } else if (tpe =:= typeOf[ZonedDateTime]) {
      schema("String", "string", ("setExample", Seq(ZonedDateTime.now().toString.qt)) +: buildFlags(flags))
    } else if (tpe <:< typeOf[Option[_]] || tpe <:< typeOf[java.util.Optional[_]]) {
      val argTpe: c.universe.Type = tpe.typeArgs.head
      primitiveSchema(argTpe, Map("setNullable" -> "true"))
    } else if (tpe <:< typeOf[Traversable[_]] || tpe <:< typeOf[java.lang.Iterable[_]]) {
      val argTpe: c.universe.Type = tpe.typeArgs.head
      primitiveSchema(argTpe, Map.empty).map { schemaArg =>
        s"""
           |{
           |  val s = new ArraySchema()
           |  s.setItems($schemaArg)
           |  s
           |}
           |""".stripMargin
      }
    } else {
      None
    }
  }

  def getRef(tpe: String, asNullable: Boolean): String = {
    val nullable: String = if (asNullable) "s.setNullable(true)" else ""
    s"""
       |{
       |  val s = new ObjectSchema
       |  $nullable
       |  s.set$$ref("$tpe")
       |  s
       |}
       |""".stripMargin
  }

  def adjustName(tpe: String): String = {
    val idx: Int = tpe.lastIndexOf(".")

    if (idx == -1) tpe
    else tpe.substring(idx+1, tpe.length)
  }

  private def extract(kind: Kind): String = kind.one match {
    case Some(value) => value
    case None => c.abort(c.enclosingPosition, "Extract kind error")
  }

  def extractSchema(tpe: c.universe.Type, store: mutable.Map[String, String] = mutable.Map.empty): String = {

    val kind: Kind = iterator(tpe, store, Map.empty)
    if (kind.primitive)
      "None"
    else {

      kind.ref match {
        case Some(value) =>
          s"""
             |{
             |  import modux.model.exporter.SchemaDescriptor
             |  import io.swagger.v3.oas.models.media._
             |  import io.swagger.v3.oas.models.parameters._
             |  Option(SchemaDescriptor($value, ${joiner(store.toMap)}))
             |}
             |""".stripMargin
        case None => c.abort(c.enclosingPosition, "extractSchema: undefined ref")
      }
    }
  }

  def iterator(tpe: c.universe.Type, store: mutable.Map[String, String], flags: Map[String, String]): Kind = {

    def routine(tpe: c.universe.Type, store: mutable.Map[String, String], flags: Map[String, String]): Kind = {
      val tps: c.universe.Symbol = tpe.typeSymbol
      val key: String = adjustName(tps.fullName)

      if (store.contains(key)) {
        val str: String = getRef(key, flags.get("setNullable").contains("true"))
        Kind(primitive = false, str, None)
      } else {

        primitiveSchema(tpe, flags) match {
          case Some(primitiveVal) => Kind(primitive = true, None, primitiveVal)
          case None =>

            if (tpe <:< typeOf[Option[_]] || tpe <:< typeOf[java.util.Optional[_]]) {
              val argTpe: c.universe.Type = tpe.typeArgs.head
              routine(argTpe, store, Map("setNullable" -> "true"))
            } else if (tpe <:< typeOf[Traversable[_]] || tpe <:< typeOf[java.lang.Iterable[_]]) {

              val argTpe: c.universe.Type = tpe.typeArgs.head
              val parameterKind: Kind = routine(argTpe, store, Map.empty)
              val result: String =
                s"""
                   |{
                   |  val s = new ArraySchema()
                   |  s.setItems(${extract(parameterKind)})
                   |  s
                   |}
                   |""".stripMargin
              Kind(primitive = true, None, result)
            } else if (tpe <:< typeOf[Map[_, _]] || tpe <:< typeOf[mutable.Map[_, _]] || tpe <:< typeOf[java.util.Map[_, _]]) {
              Kind(primitive = true, None, "new MapSchema()")
            } else {

              if (tps.isJava) {

                val mapper: Map[String, c.universe.MethodSymbol] = tpe.decls
                  .filter(x => !x.isConstructor && x.isMethod)
                  .collect { case x: MethodSymbol => x }
                  .filter { x =>
                    val name: String = x.name.toString
                    name.startsWith("set") || name.startsWith("get")
                  }
                  .foldLeft(Map[String, c.universe.MethodSymbol]()) { case (acc, x) =>
                    val name: String = x.name.toString
                    acc + (name -> x)
                  }

                val m: List[Seq[String]] = mapper
                  .map { case (k, _) => if (k.startsWith("set")) k.replaceFirst("set", "") else k.replaceFirst("get", "") }
                  .toSet.toList
                  .filter { x =>

                    (for (set <- mapper.get(s"set$x"); get <- mapper.get(s"get$x")) yield (set, get)).exists { case (set, get) =>

                      val params: List[c.universe.Symbol] = set.paramLists.flatten

                      params.length == 1 && params.head.typeSignature =:= get.returnType
                    }
                  }
                  .flatMap(x => mapper.get(s"get$x").map(y => (x, y)))
                  .map { case (name, x) =>

                    val sig: c.universe.Type = x.returnType
                    val signatureStr: String = adjustName(sig)
                    val finalName: String = name(0).toLowerCase + name.substring(1, name.length)

                    store.get(signatureStr) match {
                      case None =>
                        val kind: Kind = routine(sig, store, Map.empty)

                        kind.ref.orElse(kind.schema) match {
                          case Some(y) => Seq(finalName.qt, y)
                          case None => c.abort(c.enclosingPosition, "Invalid ref")
                        }

                      case _ => Seq(finalName.qt, getRef(signatureStr, asNullable = false))
                    }
                  }

                val result: String = schema(
                  tps.fullName,
                  "object",
                  m.map(x => ("addProperties", x))
                )

                store.put(key, result)
                Kind(primitive = false, getRef(key, false), result)
              } else if (tps.isClass) {
                val tpc: c.universe.ClassSymbol = tps.asClass

                if (tpc.isTrait) {

                  val knownDirectSubclasses: Set[c.universe.Symbol] = tpc.knownDirectSubclasses
                  val xs: Set[String] = knownDirectSubclasses.map(x => getRef(adjustName(x.fullName), asNullable = false))

                  val result: String =
                    s"""
                       |{
                       |  val s = new ComposedSchema
                       |  val xs = new java.util.ArrayList[Schema[_]]()
                       |  ${xs.map(x => s"xs.add($x)").mkString("\n")}
                       |  s.setOneOf(xs)
                       |  s
                       |}
                       |""".stripMargin

                  store.put(key, result)

                  knownDirectSubclasses.foreach { x =>
                    val typeSignature: c.universe.Type = x.typeSignature
                    routine(typeSignature, store, Map.empty)
                  }

                  Kind(primitive = false, getRef(key, false), result)
                } else {

                  val constructor: Option[c.universe.Symbol] = tpe.decls.find(_.isConstructor)
                  val params: List[c.universe.Symbol] = constructor.map(x => x.typeSignature).map(_.paramLists.flatten).getOrElse(Nil)
                  val argsParam: List[Seq[String]] = params.map { x =>

                    val sig: c.universe.Type = x.typeSignature
                    val signatureStr: String = adjustName(sig)
                    store.get(signatureStr) match {
                      case None =>

                        val kind: Kind = routine(sig, store, Map.empty)
                        kind.one match {
                          case Some(y) => Seq(x.name.toString.qt, y)
                          case None => c.abort(c.enclosingPosition, "Kind error")
                        }


                      case _ => Seq(x.name.toString.qt, getRef(signatureStr, asNullable = false))
                    }
                  }

                  val result: String = schema(
                    tpc.fullName,
                    "object",
                    argsParam.map(x => ("addProperties", x))
                  )

                  store.put(key, result)

                  Kind(primitive = false, getRef(key, false), result)
                }
              } else {
                c.abort(c.enclosingPosition, "Unsupported kind")
              }
            }
        }
      }
    }

    routine(tpe, store, flags)
  }
}

object SchemaUtils {
  implicit def asPrimitive(s: String): (Boolean, String) = (true, s)
}
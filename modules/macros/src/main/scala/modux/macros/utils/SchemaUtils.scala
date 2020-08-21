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


  def joiner(m: Map[String, String]): String = m.map { case (k, v) => s"${k.qt} -> $v" }.mkString("Map[String, MSchema](", ",", ")")

  private def strOpt(d: Option[String]): String = d match {
    case Some(value) => s"""Option("$value")"""
    case None => "None"
  }


  def matOpt(d: Option[String]): String = d match {
    case Some(value) => s"Option($value)"
    case None => "None"
  }

  def schema(kind: String, isPrimitive: Boolean, isNullable: Boolean, format: Option[String], example: Option[String], properties: Seq[(String, String)] = Nil): String = {

    s"""
       |MPrimitiveSchema(
       |  kind = "$kind",
       |  isPrimitive = $isPrimitive,
       |  isNullable = $isNullable,
       |  format = ${strOpt(format)},
       |  example = ${strOpt(example)},
       |  properties = Map(${properties.map { case (x, y) => s"${x.qt} -> $y" }.mkString(",")})
       |)
       |""".stripMargin
  }

  def buildFlags(m: Map[String, String]): Seq[(String, Seq[String])] = m.filter { case (_, v) => v == "true" }.map { case (k, v) => (k, Seq(v)) }.toSeq

  def callSchemaExtractor(tpe: c.universe.Type, store: mutable.Map[String, String] = mutable.Map.empty): Kind = iterator(tpe, store, false, false)

  def primitiveSchema(tpe: c.universe.Type, isRequired: Boolean = false, isNullable: Boolean = false): Option[String] = {

    lazy val currentTime: Long = System.currentTimeMillis()

    if (tpe =:= typeOf[String]) {
      schema("string", true, isNullable, None, None)
    } else if (tpe =:= typeOf[Int] || tpe =:= typeOf[Short]) {
      schema("integer", true, isNullable, "int32", None)
    } else if (tpe =:= typeOf[Byte]) {
      schema("byte", true, isNullable, None, None)
    } else if (tpe =:= typeOf[Long]) {
      schema("integer", true, isNullable, "int64", None)
    } else if (tpe =:= typeOf[Float]) {
      schema("number", true, isNullable, "float", None)
    } else if (tpe =:= typeOf[Double]) {
      schema("number", true, isNullable, "double", None)
    } else if (tpe =:= typeOf[Boolean]) {
      schema("boolean", true, isNullable, None, None)
    } else if (tpe =:= typeOf[Date]) {
      schema("string", true, isNullable, None, new Date().toString)
    } else if (tpe =:= typeOf[SQLDate]) {
      schema("string", true, isNullable, None, new SQLDate(currentTime).toString)
    } else if (tpe =:= typeOf[Timestamp]) {
      schema("string", true, isNullable, None, new Timestamp(currentTime).toString)
    } else if (tpe =:= typeOf[Instant]) {
      schema("string", true, isNullable, None, Instant.now().toString)
    } else if (tpe =:= typeOf[LocalDateTime]) {
      schema("string", true, isNullable, None, LocalDateTime.now().toString)
    } else if (tpe =:= typeOf[LocalDate]) {
      schema("string", true, isNullable, None, LocalDate.now().toString)
    } else if (tpe =:= typeOf[Time]) {
      schema("string", true, isNullable, None, new Time(currentTime).toString)
    } else if (tpe =:= typeOf[LocalTime]) {
      schema("string", true, isNullable, None, LocalTime.now().toString)
    } else if (tpe =:= typeOf[ZonedDateTime]) {
      schema("string", true, isNullable, None, ZonedDateTime.now().toString)
    } else if (tpe <:< typeOf[Option[_]] || tpe <:< typeOf[java.util.Optional[_]]) {
      val argTpe: c.universe.Type = tpe.typeArgs.head
      primitiveSchema(argTpe, isRequired, true)
    } else if (tpe <:< typeOf[Traversable[_]] || tpe <:< typeOf[java.lang.Iterable[_]]) {

      val argTpe: c.universe.Type = tpe.typeArgs.head

      primitiveSchema(argTpe, isRequired, isNullable)//.map(schemaArg => s"MArraySchema($schemaArg)")
    } else {
      None
    }
  }

  def getRef(tpe: String, isNullable: Boolean): String = {
    s"MRefSchema(${tpe.qt}, $isNullable)"
  }

  def adjustName(tpe: String): String = {
    val idx: Int = tpe.lastIndexOf(".")

    if (idx == -1) tpe
    else tpe.substring(idx + 1, tpe.length)
  }

  private def extract(kind: Kind): String = kind.one match {
    case Some(value) => value
    case None => c.abort(c.enclosingPosition, "Extract kind error")
  }

  def extractArraySchema(tpe: c.universe.Type, store: mutable.Map[String, String] = mutable.Map.empty): String = {
    val kind: Kind = iterator(tpe, store, false, false)
    if (kind.primitive)
      "None"
    else {

      kind.ref match {
        case Some(value) =>
          s"""
             |{
             |  import modux.model.exporter.SchemaDescriptor
             |  Option(SchemaDescriptor(MArraySchema($value), ${joiner(store.toMap)}))
             |}
             |""".stripMargin
        case None => c.abort(c.enclosingPosition, "extractSchema: undefined ref")
      }
    }
  }

  def extractSchema(tpe: c.universe.Type, store: mutable.Map[String, String] = mutable.Map.empty): String = {

    val kind: Kind = iterator(tpe, store, false, false)

    if (kind.primitive)
      "None"
    else {

      kind.ref match {
        case Some(value) =>
          s"""
             |{
             |  import modux.model.exporter.SchemaDescriptor
             |  import modux.model.schema.{MParameter, MSchema, MRefSchema, MPrimitiveSchema, MArraySchema, MComposed}
             |  Option(SchemaDescriptor($value, ${joiner(store.toMap)}))
             |}
             |""".stripMargin
        case None => c.abort(c.enclosingPosition, "extractSchema: undefined ref")
      }
    }
  }

  def iterator(tpe: c.universe.Type, store: mutable.Map[String, String], isRequired: Boolean, isNullable: Boolean): Kind = {

    def routine(tpe: c.universe.Type, store: mutable.Map[String, String], isRequired: Boolean, isNullable: Boolean): Kind = {
      val tps: c.universe.Symbol = tpe.typeSymbol
      val key: String = adjustName(tps.fullName)

      if (store.contains(key)) {
        val str: String = getRef(key, isNullable)
        Kind(primitive = false, str, None)
      } else {

        primitiveSchema(tpe, isRequired, isNullable) match {
          case Some(primitiveVal) => Kind(primitive = true, None, primitiveVal)
          case None =>

            if (tpe <:< typeOf[Option[_]] || tpe <:< typeOf[java.util.Optional[_]]) {

              val argTpe: c.universe.Type = tpe.typeArgs.head
              routine(argTpe, store, isRequired, true)
            } else if (tpe <:< typeOf[Traversable[_]] || tpe <:< typeOf[java.lang.Iterable[_]]) {

              val argTpe: c.universe.Type = tpe.typeArgs.head
              val parameterKind: Kind = routine(argTpe, store, false, false)

              Kind(primitive = true, None, s"MArraySchema(${extract(parameterKind)})")
            } else if (tpe <:< typeOf[Map[_, _]] || tpe <:< typeOf[mutable.Map[_, _]] || tpe <:< typeOf[java.util.Map[_, _]]) {
              Kind(primitive = true, None, "MPrimitiveSchema(\"object\", false, false, None, None, Map())")
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

                val m: List[(String, String)] = mapper
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
                        val kind: Kind = routine(sig, store, false, false)

                        kind.ref.orElse(kind.schema) match {
                          case Some(y) => finalName -> y
                          case None => c.abort(c.enclosingPosition, "Invalid ref")
                        }

                      case _ => finalName -> getRef(signatureStr, false)
                    }
                  }

                val result: String = schema("object", false, isNullable, None, None, m)

                store.put(key, result)
                Kind(primitive = false, getRef(key, false), result)
              } else if (tps.isClass) {
                val tpc: c.universe.ClassSymbol = tps.asClass

                if (tpc.isTrait) {

                  val knownDirectSubclasses: Set[c.universe.Symbol] = tpc.knownDirectSubclasses
                  val xs: Set[String] = knownDirectSubclasses.map(x => getRef(adjustName(x.fullName), false))
                  val result: String = s"MComposed(Seq(${xs.mkString(",")}))"

                  store.put(key, result)

                  knownDirectSubclasses.foreach { x =>
                    val typeSignature: c.universe.Type = x.typeSignature
                    routine(typeSignature, store, false, false)
                  }

                  Kind(primitive = false, getRef(key, false), result)
                } else {

                  val constructor: Option[c.universe.Symbol] = tpe.decls.find(_.isConstructor)
                  val params: List[c.universe.Symbol] = constructor.map(x => x.typeSignature).map(_.paramLists.flatten).getOrElse(Nil)
                  val argsParam: List[(String, String)] = params.map { x =>

                    val sig: c.universe.Type = x.typeSignature
                    val signatureStr: String = adjustName(sig)
                    store.get(signatureStr) match {
                      case None =>

                        val kind: Kind = routine(sig, store, false, false)
                        kind.one match {
                          case Some(y) => x.name.toString -> y
                          case None => c.abort(c.enclosingPosition, "Kind error")
                        }


                      case _ => x.name.toString -> getRef(signatureStr, false)
                    }
                  }

                  val result: String = schema("object", false, isNullable, None, None, argsParam)

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

    routine(tpe, store, isRequired, isNullable)
  }
}

object SchemaUtils {
  implicit def asPrimitive(s: String): (Boolean, String) = (true, s)
}
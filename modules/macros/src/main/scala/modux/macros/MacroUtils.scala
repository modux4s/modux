package modux.macros

import modux.model.rest.PathMetadata

import scala.reflect.macros.blackbox

object MacroUtils {

  final val nameRegex: String = "[a-z-A-Z][a-z-A-Z0-9]*"
  final val SUPPORTED_ITERABLE: Set[String] = Set("Seq", "Set", "List")

  implicit class StrUtil(x: String) {
    def qt: String = s""""$x""""
  }

  implicit def toOption(x: String): Option[String] = Option(x)

  implicit def asStr[T](x: T): String = x.toString

  def fullName(c: blackbox.Context)(x: c.universe.Type): String = x.typeConstructor.typeSymbol.fullName

  def same(c: blackbox.Context)(x: c.universe.Type, y: c.universe.Type*): Boolean = y.exists(_.typeConstructor == x.typeConstructor)

  def normalizePath(x: String): String = PathMetadata.normalizePath(x)

  //noinspection RegExpRedundantEscape
  def extractVariableName(c: blackbox.Context)(url: String): PathMetadata = PathMetadata.parser(url) match {
    case Left(value) => c.abort(c.enclosingPosition, value)
    case Right(value) => value
  }

  def extractSubType(tpe: String): String = {
    val start: Int = tpe.indexOf('[')
    val end: Int = tpe.lastIndexOf(']')
    if (start > -1 && end > -1) {
      tpe.substring(start + 1, end)
    } else {
      tpe
    }
  }

  def extractSuperType(tpe: String): String = {
    val start: Int = tpe.indexOf('[')
    if (start > -1) {
      tpe.substring(0, start)
    } else {
      tpe
    }
  }

  def isIterable(tpe: String): Boolean = {

    SUPPORTED_ITERABLE.exists { x =>
      println(x)
      tpe.startsWith(x)
    }
  }
}

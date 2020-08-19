package modux.macros

import modux.model.{AsPath, AsPathParam, Path, PathMetadata}

import scala.reflect.macros.blackbox
import scala.util.matching.Regex

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

  def normalizePath(x: String): String = {
    val v1: String = if (x.startsWith("/")) x else s"/$x"
    val idx: Int = v1.indexOf("?")

    if (idx == -1) v1 else v1.substring(0, idx)
  }

  //noinspection RegExpRedundantEscape
  def extractVariableName(c: blackbox.Context)(url: String): PathMetadata = {

    def parsePartialUrl(x: String): Seq[Path] = {
      x.split("/").map { x =>
        val bool1: Boolean = x.startsWith(":")

        if (bool1) {
          AsPathParam(x.substring(1, x.length))
        } else if (!bool1 && !x.contains(":")) {
          AsPath(x)
        } else {
          c.abort(c.enclosingPosition, s"Invalid path section: '$x'")
        }
      }
    }

    def parseQueryParam(x: String): Seq[String] = if (x.trim.isEmpty) Nil else x.split("&")

    def splitParams(url: String): (String, String) = {
      url.split("\\?") match {
        case Array(a) => (a, "")
        case Array(a, b) => (a, b)
        case _ => c.abort(c.enclosingPosition, s"Invalid url $url")
      }
    }

    val finalUrl: String = if (url.startsWith("/")) {
      url.substring(1)
    } else {
      url
    }

    val (pathParams, queryParams) = splitParams(finalUrl)
    PathMetadata(finalUrl, parsePartialUrl(pathParams), parseQueryParam(queryParams))
  }
}

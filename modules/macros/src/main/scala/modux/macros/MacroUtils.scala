package modux.macros

import modux.model.rest.{AnythingPath, AsPath, AsPathParam, Path, PathMetadata, AsRegexPath}

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

  def normalizePath(x: String): String = {
    val v1: String = if (x.startsWith("/")) x else s"/$x"
    val idx: Int = v1.indexOf("?")

    if (idx == -1) v1 else v1.substring(0, idx)
  }

  //noinspection RegExpRedundantEscape
  def extractVariableName(c: blackbox.Context)(url: String): PathMetadata = {

    def parsePartialUrl(x: String): Seq[Path] = {
      val result: (Seq[Path], Seq[Path]) = x
        .split("/")
        .toSeq
        .map { x =>

          val isParamRef: Boolean = x.startsWith(":")
          val isParamRegex: Boolean = x.startsWith(":") && x.indexOf('<') < x.indexOf('>') && x.endsWith(">") && x.count(_ == '<') == 1 && x.count(_ == '>') == 1

          if (isParamRef) {
            AsPathParam(x.substring(1, x.length))
          } else if (x == "*") {
            AnythingPath
          } else if (isParamRegex) {
            val startIdx: Int = x.indexOf('<')
            val endIdx: Int = x.indexOf('>')
            val name: String = x.substring(1, startIdx)
            val regex: String = x.substring(startIdx, endIdx - 1)

            AsRegexPath(name, regex)
          } else if (!isParamRef && !isParamRegex && !x.contains(":")) {
            AsPath(x)
          } else {
            c.abort(c.enclosingPosition, s"Invalid path section: '$x'")
          }
        }
        .span {
          case AnythingPath => false
          case _ => true
        }

      result match {
        case (h, Nil) => h
        case (h, t) => h :+ t.head
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

    def validatePathParams(pathParams: String): Unit = {
      val count: Int = pathParams.count(_ == '*')
      if (count <= 1) {
        if (count >= 1 && !pathParams.endsWith("*")) {
          c.abort(c.enclosingPosition, "Incorrect '*' pattern applied. Use it at the end of path. ")
        }
      } else {
        c.abort(c.enclosingPosition, "Incorrect '*' pattern applied. Use it at the end of path. ")
      }
    }

    val finalUrl: String = if (url.startsWith("/")) {
      url.substring(1)
    } else {
      url
    }

    val (pathParams, queryParams) = splitParams(finalUrl)

    validatePathParams(pathParams)

    PathMetadata(finalUrl, parsePartialUrl(pathParams), parseQueryParam(queryParams))
  }
}

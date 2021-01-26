package modux.model.rest

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

sealed trait Path {
  def name: String
}

/** handle part of path */
final case class AsPath(name: String) extends Path

/** handle :id */
final case class AsPathParam(name: String) extends Path

/** handle * at the end */
object AnythingPath extends Path {
  val name: String = "*"
}

/** handle :id<regex> */
final case class AsRegexPath(name: String, value: String) extends Path

final case class PathMetadata(url: String, pathParams: Seq[Path], queryParams: Seq[String]) {
  lazy val parsedArguments: Seq[Path] = pathParams.collect {
    case x: AsPathParam => x
    case AnythingPath => AnythingPath
    case x: AsRegexPath => x
  }

  lazy val parsedArgumentsMap: Map[String, Path] = parsedArguments.map(x => x.name -> x).toMap

  lazy val hasAnything: Boolean = pathParams.exists {
    case AnythingPath => true
    case _ => false
  }
}

object PathMetadata {
  def normalizePath(x: String): String = {
    val v1: String = if (x.startsWith("/")) x else s"/$x"
    val idx: Int = v1.indexOf("?")

    if (idx == -1) v1 else v1.substring(0, idx)
  }

  def parser(url: String): Either[String, PathMetadata] = {
    def parsePartialUrl(x: String): Either[String, Seq[Path]] = {
      val result: Try[Either[String, List[Path]]] = x
        .split("/")
        .toSeq
        .map { x =>

          val countDblPoint: Boolean = x.count(_ == ':') == 1
          val isParamRef: Boolean = x.startsWith(":") && countDblPoint
          val isParamRegex: Boolean = x.startsWith(":") && countDblPoint && x.indexOf('<') < x.indexOf('>') && x.endsWith(">") && x.count(_ == '<') == 1 && x.count(_ == '>') == 1

          if (isParamRef) {
            Success(AsPathParam(x.substring(1, x.length)))
          } else if (x == "*") {
            Success(AnythingPath)
          } else if (isParamRegex) {
            val startIdx: Int = x.indexOf('<')
            val endIdx: Int = x.indexOf('>')
            val name: String = x.substring(1, startIdx)
            val regex: String = x.substring(startIdx, endIdx - 1)

            Success(AsRegexPath(name, regex))
          } else if (!isParamRef && !isParamRegex && !x.contains(":")) {
            Success(AsPath(x))
          } else {
            Failure(new Exception(s"Invalid path section: '$x'"))
          }
        }
        .foldLeft(Try(ArrayBuffer.empty[Path])) { case (acc, item) =>
          for (b <- acc; it <- item) yield b += it
        }
        .map { xs =>
          xs.span {
            case AnythingPath => false
            case _ => true
          }
        }
        .map { case (paths, paths1) => (paths.toList, paths1.toList) }
        .map {
          case (h, Nil) => Right(h)
          case (h, t) => Right(h :+ t.head)
        }

      result match {
        case Failure(exception) => Left(exception.getMessage)
        case Success(value) => value
      }
    }

    def parseQueryParam(x: String): Seq[String] = if (x.trim.isEmpty) Nil else x.split("&")

    def splitParams(url: String): Either[String, (String, String)] = {
      url.split("\\?") match {
        case Array(a) => Right((a, ""))
        case Array(a, b) => Right((a, b))
        case _ => Left(s"Invalid url $url")
      }
    }

    def validatePathParams(pathParams: String): Either[String, String] = {
      val count: Int = pathParams.count(_ == '*')
      if (count <= 1) {
        if (count >= 1 && !pathParams.endsWith("*")) {
          Left("Incorrect '*' pattern applied. Use it at the end of path. ")
        } else {
          Right(pathParams)
        }
      } else {
        Left("Incorrect '*' pattern applied. Use it at the end of path. ")
      }
    }

    val finalUrl: String = if (url.startsWith("/")) {
      url.substring(1)
    } else {
      url
    }

    splitParams(finalUrl).flatMap { case (pathParams, queryParams) =>
      for {
        _ <- validatePathParams(pathParams)
        q <- parsePartialUrl(pathParams)
      } yield PathMetadata(finalUrl, q, parseQueryParam(queryParams))
    }
  }

  def swaggerFormat(url: String): String = {
    parser(url)
      .map { met =>

        val result: String = met
          .pathParams
          .collect {
            case AsPath(name) => name
            case AsPathParam(name) => s"{$name}"
            case AsRegexPath(name, _) => s"{$name}"
          }.mkString("/")

        "/" + result
      }
      .getOrElse(url)
  }
}
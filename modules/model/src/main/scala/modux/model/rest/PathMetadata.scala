package modux.model.rest

sealed trait Path {
  def name: String
}

/** handle :id */
final case class AsPath(name: String) extends Path

/** handle ?param1&param2&...&paramN */
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

package modux.model

sealed trait Path

case class AsPath(name: String) extends Path

case class AsPathParam(name: String) extends Path

case class PathMetadata(url: String, pathParams: Seq[Path], queryParams: Seq[String]) {
  lazy val parsedArguments: Seq[AsPathParam] = pathParams.collect { case x: AsPathParam => x }
  lazy val parsedArgumentsMap: Map[String, AsPathParam] = parsedArguments.map(x => x.name -> x).toMap
}

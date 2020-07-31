package modux.shared

case class ServerDecl(url: String, description: String, variables: java.util.Map[String, ServerVar])

object ServerDecl{
  import scala.collection.JavaConverters._

  def apply(url: String, description: String, variables: Map[String, ServerVar]): ServerDecl = new ServerDecl(url, description, variables.asJava)
}
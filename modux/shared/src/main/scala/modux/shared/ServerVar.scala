package modux.shared

case class ServerVar(default: String, values: java.util.List[String])

object ServerVar {

  import scala.collection.JavaConverters._

  def apply(default: String, values: String*): ServerVar = new ServerVar(default, values.asJava)
}
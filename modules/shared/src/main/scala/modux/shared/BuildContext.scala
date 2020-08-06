package modux.shared

import java.util

case class BuildContext(settings: java.util.Map[String, String], appClassloader: ClassLoader, servers: java.util.List[ServerDecl] = new util.ArrayList[ServerDecl]()) {
  def get(key: String): String = settings.get(key)
}

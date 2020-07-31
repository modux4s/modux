package modux.common.utils

object StrUtils {
  def ident(n: Int, s: String, sep: String = "  "): String = (sep * n) + s

  def qt(s: String): String = "\"" + s + "\""

  implicit def asStr[T](x: T): String = x.toString
}

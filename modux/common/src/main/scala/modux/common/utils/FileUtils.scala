package modux.common.utils

import scala.io.{BufferedSource, Source}

object FileUtils {
  def readFileContent(path: String, separator: String = "\n"): String = {
    val source: BufferedSource = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(path))
    val result: String = source.getLines().mkString(separator)
    source.close()
    result
  }
}

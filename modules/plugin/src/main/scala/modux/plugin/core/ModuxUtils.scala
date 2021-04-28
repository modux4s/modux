package modux.plugin.core

import scala.io.StdIn

private[modux] object ModuxUtils {

  def waitForEnter(): Unit = {
    StdIn.readLine()
  }

}

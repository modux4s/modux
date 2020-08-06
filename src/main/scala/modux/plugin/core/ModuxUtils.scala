package modux.plugin.core

import sbt.nio.Watch

import scala.annotation.tailrec

private[modux] object ModuxUtils {

  @tailrec
  def waitForEnter(waitTime: Long): Unit = {
    if (System.in.available() == 0 || System.in.read().toChar != '\n') {
      Thread.sleep(waitTime)
      waitForEnter(waitTime)
    }
  }

}

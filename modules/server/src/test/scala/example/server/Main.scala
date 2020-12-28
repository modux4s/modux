package example.server

import modux.server.service.ModuxServer

object Main {
  def main(args: Array[String]): Unit = {
    ModuxServer("test", "localhost", 9000, this.getClass.getClassLoader)
  }
}

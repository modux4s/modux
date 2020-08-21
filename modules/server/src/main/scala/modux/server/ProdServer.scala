package modux.server

import modux.server.service.ModuxServer

import scala.io.StdIn

object ProdServer extends App {
  private val name: String = sys.props.getOrElse("modux.name", "defaul")
  private val host: String = sys.props.getOrElse("modux.host", "localhost")
  private val port: String = sys.props.getOrElse("modux.port", "9000")
  private val server = ModuxServer(name, host, port.toInt, this.getClass.getClassLoader)
  println(s"[info] Server up $host:$port")
  println("[info] Press enter to exit...")
  StdIn.readLine()
  println("[info] Shouting down...")
  server.stop()
}

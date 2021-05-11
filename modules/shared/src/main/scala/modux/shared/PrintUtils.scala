package modux.shared

object PrintUtils {

  def red(msg: String): Unit = println(Console.RED + msg + Console.RESET)

  def green(msg: String): Unit = println(Console.GREEN + msg + Console.RESET)

  def green_b(msg: String): Unit = println(Console.GREEN_B + msg + Console.RESET)

  def cyan(msg: String): Unit = println(Console.CYAN + msg + Console.RESET)

  def white(msg: String): Unit = println(msg)

  def info(msg: String): Unit = println(s"[info] $msg")

  def success(msg: String): Unit = println(s"[${Console.GREEN}success${Console.RESET}] $msg")

  def error(msg: String): Unit = println(s"[${Console.RED}error${Console.RESET}] $msg")
}

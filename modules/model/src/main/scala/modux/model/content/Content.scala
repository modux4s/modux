package modux.model.content

import akka.http.scaladsl.marshalling.ToEntityMarshaller


sealed trait Content {
  def status: Int
  def headers: Map[String, String]
  def cookies: Map[String, String]
}

final case class WithContent[T](status: Int, body: T, headers: Map[String, String] = Map.empty, cookies: Map[String, String] = Map.empty)(implicit m: ToEntityMarshaller[T]) extends Content {

  val marshaller: ToEntityMarshaller[T] = m

  def withHeader(n: String, v: String): WithContent[T] = copy(headers = headers + (n -> v))

  def withCookie(n: String, v: String): WithContent[T] = copy(cookies = cookies + (n -> v))
}

final case class WithoutContent(status: Int, message: Option[String] = None, headers: Map[String, String] = Map.empty, cookies: Map[String, String] = Map.empty) extends Content {
  def withHeader(n: String, v: String): WithoutContent = copy(headers = headers + (n -> v))

  def withCookie(n: String, v: String): WithoutContent = copy(cookies = cookies + (n -> v))

  def withMessage(v: String): WithoutContent = copy(message = Option(v))
}
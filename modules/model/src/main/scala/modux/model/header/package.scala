package modux.model

import akka.http.scaladsl.model.ContentType

package object header {

  sealed trait ContentAs

  case object Default extends ContentAs

  case object Json extends ContentAs

  case object Xml extends ContentAs

  case object TextPlain extends ContentAs

  case object Html extends ContentAs

  case class Custom(contentType: ContentType) extends ContentAs
}

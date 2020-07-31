package modux.model.dsl

import akka.http.scaladsl.marshalling.ToResponseMarshallable


final case class ModuxResponseException(data: ToResponseMarshallable) extends RuntimeException

package modux.model.dsl

import akka.http.scaladsl.marshalling.ToResponseMarshallable


final case class ResponseAsFalseFail(data: ToResponseMarshallable) extends RuntimeException

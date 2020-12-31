package modux.core.feature.graphql

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import modux.model.service.Call

trait GraphQLInterpreter {
  def service: Call[Option[String], Source[ByteString, NotUsed]]
}

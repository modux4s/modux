package modux.model.rest

import akka.http.scaladsl.server.Route
import modux.model.dsl.RestEntryExtension

trait RestInstance extends RestService {
  def route(extensions: Seq[RestEntryExtension]): Route
}

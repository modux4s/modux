package modux.core.server.domain

import akka.http.scaladsl.server.Route
import modux.core.api.ModuleX
import modux.model.ServiceDescriptor

import scala.collection.mutable

case class Capture(routes: Route, modules: mutable.ArrayBuffer[ModuleX], servicesSpec: mutable.ArrayBuffer[ServiceDescriptor])
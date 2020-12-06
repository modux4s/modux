package modux.server.model

import akka.http.scaladsl.server.Route
import modux.core.api.ModuleX
import modux.model.{ServiceDef, ServiceEntry}

import scala.collection.mutable

final case class Capture(routes: Route, modules: mutable.ArrayBuffer[ModuleX], servicesSpec: mutable.ArrayBuffer[ServiceDef], servicesEntry: mutable.ArrayBuffer[ServiceEntry])

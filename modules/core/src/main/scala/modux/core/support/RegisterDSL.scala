package modux.core.support

import modux.model.dsl._
import modux.model.{ServiceDef, ServiceEntry}

trait RegisterDSL extends SupportUtils {

  def serviceDef: ServiceDef

  protected def namedAs(serviceName: String): ServiceDef = ServiceDef(serviceName)

  protected def namespace(ns: String)(restEntry: RestEntry*): ServiceEntry = NameSpacedEntry(ns, restEntry)

}

package modux.server.model

import modux.model.ServiceDef
import modux.shared.PrintUtils

object DevInfo {

  def apply(descriptor: ServiceDef): Unit = {

    val servicesCallSize: Int = descriptor.servicesCall.size

    val description: String = {
      if (servicesCallSize == 0) {
        s"${descriptor.name} no contains services."
      } else if (servicesCallSize == 1) {
        s"${descriptor.name} contains one service."
      } else {
        s"${descriptor.name} contains $servicesCallSize services."
      }
    }

    PrintUtils.info(description)
  }
}

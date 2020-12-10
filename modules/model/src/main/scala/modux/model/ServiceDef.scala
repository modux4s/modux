package modux.model

final case class ServiceDef private(name: String, namespace: Option[String] = None, servicesCall: Seq[ServiceEntry] = Nil)

package modux.model

case class ServiceDescriptor private(name: String, namespace: Option[String] = None, servicesCall: Seq[ServiceEntry] = Nil)

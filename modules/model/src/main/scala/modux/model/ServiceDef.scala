package modux.model

final case class ServiceDef private(name: String, serviceEntries: Seq[ServiceEntry] = Nil) {

  def entry(item: ServiceEntry*): ServiceDef = {
    ServiceDef(name, serviceEntries ++ item)
  }

}

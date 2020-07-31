package modux.model.exporter

sealed trait MetaTree

final case class MetaDataNode(name: String, items: MetaTree*) extends MetaTree

final case class MetaValue(value: String) extends MetaTree

final case class MetaKeyValue(name: String, value: String) extends MetaTree

final case class MetaList(items: MetaTree) extends MetaTree

final case class QueryParamDescriptor(
                                       name: String,
                                       kind: String,
                                       isOpt: Boolean,
                                       isIterable: Boolean
                                     )

final case class PathParamDescriptor(name:String, kind:String)
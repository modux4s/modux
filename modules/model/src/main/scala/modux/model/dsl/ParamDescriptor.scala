package modux.model.dsl

case class ParamDescriptor(_kind:ParamKind, _description: Option[String], _examples: Seq[(String, String)], restEntry: RestEntry) {
  def as(x: String): ParamDescriptor = ParamDescriptor(_kind, Option(x), _examples, restEntry)

  def withExamples(x: (String, String)*): ParamDescriptor = ParamDescriptor(_kind, _description, x, restEntry)
}

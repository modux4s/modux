package modux.model.dsl

case class ExampleContent(map:Map[String, String] = Map.empty){
  def asMap: Map[String, String] = map
}

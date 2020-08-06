package modux.model.dsl

import modux.model.exporter.MediaTypeDescriptor

case class ResponseDescriptor(code: Int, schema: Option[MediaTypeDescriptor], description: String, example: ExampleContent) {
  def sample(x: ExampleContent): ResponseDescriptor = ResponseDescriptor(code, schema, description, x)
}
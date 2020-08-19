package modux.model.dsl

import modux.model.exporter.MediaTypeDescriptor

case class ResponseDescriptor(code: Int, schema: Option[MediaTypeDescriptor], description: String, _example: ExampleContent) {
  def withExample(x: ExampleContent): ResponseDescriptor = ResponseDescriptor(code, schema, description, x)
}
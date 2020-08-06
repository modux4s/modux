package modux.model.dsl

import modux.model.exporter.MediaTypeDescriptor

case class RequestDescriptor(mediaType: MediaTypeDescriptor, description: Option[String] = None, example: ExampleContent = ExampleContent()) {
}

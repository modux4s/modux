package modux.model

import modux.model.exporter.SchemaDescriptor
import modux.model.schema.{MParameter, MSchema}


trait RestProxy extends RestService {

  def ignore: Boolean

  def path: String

  def method: String

  def schemas: Map[String, MSchema]

  def pathParameter: Seq[MParameter]

  def queryParameter: Seq[MParameter]

  def requestWith: Option[SchemaDescriptor] = None

  def responseWith: Option[SchemaDescriptor] = None
}
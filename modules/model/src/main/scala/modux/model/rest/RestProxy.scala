package modux.model.rest

import modux.model.exporter.SchemaDescriptor
import modux.model.schema.{MParameter, MSchema}

final case class RestProxy(
                            path: String,
                            method: String,
                            schemas: Map[String, MSchema],
                            pathParameter: Seq[MParameter],
                            queryParameter: Seq[MParameter],
                            ignore: Boolean = false,
                            requestWith: Option[SchemaDescriptor] = None,
                            responseWith: Option[SchemaDescriptor] = None
                          ) extends RestService {

}


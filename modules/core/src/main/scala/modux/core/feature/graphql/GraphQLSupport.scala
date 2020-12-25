package modux.core.feature.graphql

import akka.NotUsed
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import jsoft.graphql.core.{EncoderTypeDerivation, GraphQL, StructTypeDerivation}
import jsoft.graphql.model.{Binding, Interpreter}
import modux.model.dsl.{RestEntry, RestEntryExtension}
import modux.model.rest.RestInstance

trait GraphQLSupport extends EncoderTypeDerivation with StructTypeDerivation {

  private final val JSON = MediaTypes.`application/json`
  private final val TEXT = MediaTypes.`text/plain`

  final implicit val marshaller: Marshaller[Source[ByteString, NotUsed], RequestEntity] = {
    Marshaller.oneOf[Source[ByteString, NotUsed], RequestEntity](
      Marshaller.withFixedContentType(JSON) { s => HttpEntity(JSON.toContentType, s) },
      Marshaller.withOpenCharset(TEXT) { (s, cs) => HttpEntity(TEXT.withCharset(cs), s) },
    )
  }

  def graphql(name: String, binding: Binding, enableSchemaValidation: Boolean = true, enableIntrospection: Boolean = true): RestEntry = {
    import akka.http.scaladsl.server.Directives._
    val interpreter: Interpreter = GraphQL.interpreter(binding, enableSchemaValidation = enableSchemaValidation, enableIntrospection = enableIntrospection)

    RestEntry(
      new RestInstance {
        override def route(extensions: Seq[RestEntryExtension]): Route = {
          path(name) {

            parameterMap { queryParams =>
              get {
                complete(200, interpreter.asAkkaSource(queryParams, None))
              } ~
                post {
                  entity(as[Option[String]]) { body =>
                    complete(200, interpreter.asAkkaSource(queryParams, body))
                  }
                }
            }
          }
        }
      }
    )

  }

}

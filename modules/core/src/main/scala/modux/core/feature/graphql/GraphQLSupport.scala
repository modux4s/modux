package modux.core.feature.graphql

import akka.NotUsed
import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import akka.util.ByteString
import jsoft.graphql.core.{EncoderTypeDerivation, GraphQL, StructTypeDerivation}
import jsoft.graphql.model.{Binding, Interpreter}
import modux.model.service.Call

import scala.concurrent.ExecutionContext

trait GraphQLSupport extends EncoderTypeDerivation with StructTypeDerivation {

  private final val JSON = MediaTypes.`application/json`
  private final val TEXT = MediaTypes.`text/plain`

  final implicit val marshaller: Marshaller[Source[ByteString, NotUsed], RequestEntity] = {
    Marshaller.oneOf[Source[ByteString, NotUsed], RequestEntity](
      Marshaller.withFixedContentType(JSON) { s => HttpEntity(JSON.toContentType, s) },
      Marshaller.withOpenCharset(TEXT) { (s, cs) => HttpEntity(TEXT.withCharset(cs), s) },
    )
  }

  def graphql(binding: => Binding, enableSchemaValidation: Boolean = true, enableIntrospection: Boolean = true)(implicit ec: ExecutionContext): GraphQLInterpreter = {
    import modux.model.service.CallDirectives._
    val interpreter: Interpreter = GraphQL.interpreter(binding, enableSchemaValidation = enableSchemaValidation, enableIntrospection = enableIntrospection)

    val impl: Call[Option[String], Source[ByteString, NotUsed]] = extractMethod { method =>

      val methodValue: String = method.value

      if (methodValue == "GET" || methodValue == "POST") {
        handleRequest { req =>
          extractInput { input =>
            interpreter.asAkkaSource(req.uri.query().toMap, input)
          }
        }
      } else {
        ???
      }
    }

    new GraphQLInterpreter {
      override def service: Call[Option[String], Source[ByteString, NotUsed]] = impl
    }

  }

}

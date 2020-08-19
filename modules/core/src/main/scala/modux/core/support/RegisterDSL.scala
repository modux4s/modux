package modux.core.support

import java.util.concurrent.TimeUnit

import akka.Done
import akka.http.scaladsl.marshalling.{Marshaller, Marshalling, ToByteStringMarshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{HttpCharsets, HttpEntity, HttpRequest, MessageEntity}
import akka.http.scaladsl.unmarshalling.{FromByteStringUnmarshaller, FromEntityUnmarshaller, Unmarshal, Unmarshaller}
import akka.stream.Materializer
import akka.util.ByteString
import modux.core.feature.{CircuitBreakExtension, RetryExtension}
import modux.macros.serializer.SerializationSupport
import modux.macros.serializer.codec.CodecRegistry
import modux.macros.serializer.codec.providers.api.CodecEntityProvider
import modux.model.context.Context
import modux.model.dsl._
import modux.model.exporter.{EvidenceDescriptor, MediaTypeDescriptor, SchemaDescriptor}
import modux.model.{RestService, ServiceDef}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try


trait RegisterDSL {

  private final val FINITE_DURATION: FiniteDuration = FiniteDuration(30, TimeUnit.SECONDS)

  protected final val pathParam: String => ParamKind = x => PathKind(x)
  protected final val cookie: String => ParamKind = x => CookieKind(x)
  protected final val header: String => ParamKind = x => HeaderKind(x)
  protected final val queryParam: String => ParamKind = x => QueryKind(x)

  protected def circuitBreak(
                              maxFailure: Int = 10,
                              callTimeout: FiniteDuration = FiniteDuration(10, TimeUnit.SECONDS),
                              resetTimeout: FiniteDuration = FiniteDuration(60, TimeUnit.SECONDS)
                            )(implicit context: Context): RestEntryExtension = CircuitBreakExtension(maxFailure, callTimeout, resetTimeout)

  protected def retry(attempts: Int = 10)(implicit context: Context): RestEntryExtension = RetryExtension(attempts)

  def serviceDef: ServiceDef

  protected def namedAs(name: String): ServiceDef = ServiceDef(name)

  protected implicit class ServiceSupportDSL(srv: ServiceDef) {

    def withNamespace(ns: String): ServiceDef = ServiceDef(
      srv.name,
      Option(ns),
      srv.servicesCall
    )

    def withCalls(xs: RestEntry*): ServiceDef = srv.copy(servicesCall = xs)
  }

  protected implicit def asRestEntry(restInstance: RestService): RestEntry = RestEntry(restInstance)

  protected implicit def asRequestDescriptor[A](schemaDescriptor: Option[SchemaDescriptor])(implicit codecRegistry: CodecRegistry = SerializationSupport.DefaultCodecRegistry): Option[RequestDescriptor] = {
    schemaDescriptor.map { x =>
      RequestDescriptor(MediaTypeDescriptor(extractMediasType(codecRegistry), x))
    }
  }

  protected implicit def toResponseDescriptor(x: CodeDescriptor): ResponseDescriptor = ResponseDescriptor(x.code, None, x.description, ExampleContent())

  protected implicit class ResponseDescriptorUtils(x: Int) {
    def ->(v: String): CodeDescriptor = CodeDescriptor(x, v)
  }

  protected implicit class CodeDescriptorUtils(x: CodeDescriptor) {
    def represented(schemaDescriptor: Option[SchemaDescriptor])(implicit codecRegistry: CodecRegistry = SerializationSupport.DefaultCodecRegistry): ResponseDescriptor = {

      val media: Option[MediaTypeDescriptor] = schemaDescriptor.map { x => MediaTypeDescriptor(extractMediasType(codecRegistry), x) }

      ResponseDescriptor(x.code, media, x.description, ExampleContent())
    }
  }

  protected implicit def asExample[T](d: T)(implicit mar: ToByteStringMarshaller[T], ec: ExecutionContext, mf: Manifest[T]): ExampleContent = {
    val maybeFuture: Future[ExampleContent] = mar(d)
      .map { x =>
        x.flatMap {
          case Marshalling.WithFixedContentType(contentType, marshal) =>
            Option(contentType.mediaType.toString() -> marshal().utf8String)
          case Marshalling.WithOpenCharset(mediaType, marshal) =>
            Option(mediaType.toString() -> marshal(HttpCharsets.`UTF-8`).utf8String)
          case _ => None
        }
      }
      .map(x => ExampleContent(x.toMap))

    Await.result(maybeFuture, FINITE_DURATION)
  }

  protected implicit def fromParameterDescToRestEntry(x: ParamDescriptor): RestEntry = {
    x.restEntry._paramDescriptor.append(x)
    x.restEntry
  }

  protected implicit class EntityUtils[T](d: T) {
    def asFuture(implicit ec: ExecutionContext): Future[T] = Future(d)
  }

  private def extractMediasType(codecRegistry: CodecRegistry): Seq[String] = codecRegistry.codecs.collect { case x: CodecEntityProvider => x }.map(_.mediaType.toString())
}

package modux.core.support

import java.util.concurrent.TimeUnit

import modux.macros.serializer.codec.CodecRegistry
import modux.macros.serializer.codec.providers.api.CodecEntityProvider
import modux.model.dsl._
import modux.model.exporter.{MediaTypeDescriptor, SchemaDescriptor}
import modux.model.{RestService, ServiceDescriptor}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}
import scala.util.Try


trait RegisterDSL {

  protected final val path: String => ParamKind = x => PathKind(x)
  protected final val cookie: String => ParamKind = x => CookieKind(x)
  protected final val header: String => ParamKind = x => HeaderKind(x)
  protected final val query: String => ParamKind = x => QueryKind(x)

  def serviceDescriptor: ServiceDescriptor

  protected def namedAs(name: String): ServiceDescriptor = ServiceDescriptor(name)

  protected implicit class ServiceSupportDSL(srv: ServiceDescriptor) {

    def withNamespace(ns: String): ServiceDescriptor = ServiceDescriptor(
      srv.name,
      Option(ns),
      srv.servicesCall
    )

    def withCalls(xs: RestEntry*): ServiceDescriptor = srv.copy(servicesCall = xs)
  }

  protected implicit def asRestEntry(restInstance: RestService): RestEntry = RestEntry(restInstance)

  protected implicit def asRequestDescriptor(schemaDescriptor: Option[SchemaDescriptor])(implicit codecRegistry: CodecRegistry): Option[RequestDescriptor] = schemaDescriptor.map { x =>
    RequestDescriptor(MediaTypeDescriptor(extractMediasType(codecRegistry), x))
  }

  protected implicit def toResponseDescriptor(x: CodeDescriptor): ResponseDescriptor = ResponseDescriptor(x.code, None, x.description, ExampleContent())

  protected implicit class ResponseDescriptorUtils(x: Int) {
    def summary(v: String): CodeDescriptor = CodeDescriptor(x, v)
  }

  protected implicit class CodeDescriptorUtils(x: CodeDescriptor) {
    def returns(schemaDescriptor: Option[SchemaDescriptor])(implicit codecRegistry: CodecRegistry): ResponseDescriptor = {

      val media: Option[MediaTypeDescriptor] = schemaDescriptor.map { x => MediaTypeDescriptor(extractMediasType(codecRegistry), x) }

      ResponseDescriptor(x.code, media, x.description, ExampleContent())
    }
  }

  protected implicit def asExample[T](d: T)(implicit codecRegistry: CodecRegistry, ec: ExecutionContext, mf: Manifest[T]): ExampleContent = {
    val xs: Seq[(String, String)] = codecRegistry.codecs.collect { case x: CodecEntityProvider => x }.flatMap { x =>
      Try(Await.result(x.toByteString[T](d), Duration(10, TimeUnit.SECONDS))).toOption.map(r => x.mediaType.toString() -> r.utf8String)
    }
    ExampleContent(xs.toMap)
  }

  protected implicit def fromParameterDescToRestEntry(x: ParamDescriptor): RestEntry = {
    x.restEntry._paramDescriptor.append(x)
    x.restEntry
  }

  private def extractMediasType(codecRegistry: CodecRegistry): Seq[String] = codecRegistry.codecs.collect { case x: CodecEntityProvider => x }.map(_.mediaType.toString())
}

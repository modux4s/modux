package modux.model.dsl

import modux.model._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


final case class RestEntry private[modux](
                                           instance: RestService,
                                           _description: Option[String] = None,
                                           _expect: Option[RequestDescriptor] = None,
                                           _response: Seq[ResponseDescriptor] = Nil,
                                           _paramDescriptor: ArrayBuffer[ParamDescriptor] = mutable.ArrayBuffer.empty[ParamDescriptor],
                                           _extensions: Seq[RestEntryExtension] = Nil,
                                         ) extends ServiceEntry {

  def summary(x: String): RestEntry = RestEntry(instance, Option(x), _expect, _response, _paramDescriptor, _extensions)

  def describe(x: ParamKind): ParamDescriptor = ParamDescriptor(x, None, Nil, this)

  def expects(x: Option[RequestDescriptor]): RestEntry = RestEntry(instance, _description, x, _response, _paramDescriptor, _extensions)

  def returns(x: ResponseDescriptor*): RestEntry = RestEntry(instance, _description, _expect, x, _paramDescriptor, _extensions)

  def extendedBy(ext: RestEntryExtension*): RestEntry = RestEntry(instance, _description, _expect, _response, _paramDescriptor, ext)
}

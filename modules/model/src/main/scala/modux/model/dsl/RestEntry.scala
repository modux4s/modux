package modux.model.dsl

import modux.model._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


final case class RestEntry private[modux](
                                           instance: RestService,
                                           _description: Option[String] = None,
                                           _expect: Option[RequestDescriptor] = None,
                                           _response: Seq[ResponseDescriptor] = Nil,
                                           _paramDescriptor: ArrayBuffer[ParamDescriptor] = mutable.ArrayBuffer.empty[ParamDescriptor]
                                         ) extends ServiceEntry {

  def summary(x: String): RestEntry = RestEntry(instance, Option(x), _expect, _response, _paramDescriptor)

  def describe(x: ParamKind): ParamDescriptor = ParamDescriptor(x, None, Nil, this)

  def expect(x: Option[RequestDescriptor]): RestEntry = RestEntry(instance, _description, x, _response, _paramDescriptor)

  def response(x: ResponseDescriptor*): RestEntry = RestEntry(instance, _description, _expect, x, _paramDescriptor)
}

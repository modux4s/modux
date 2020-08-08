package modux.model

import akka.http.scaladsl.server.Route
import akka.pattern.CircuitBreaker
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import modux.model.exporter.SchemaDescriptor

trait RestService

trait RestInstance extends RestService {
  def route: Route
  def withCircuitBreak(circuitBreaker: CircuitBreaker): Route
}

trait RestProxy extends RestService {

  def ignore: Boolean

  def path: String

  def method: String

  def schemas: Map[String, Schema[_]]

  def pathParameter: Seq[Parameter]

  def queryParameter: Seq[Parameter]

  def requestWith: Option[SchemaDescriptor] = None

  def responseWith: Option[SchemaDescriptor] = None

  override def toString: String = {
    s"""
       |path:  ${this.path}
       |method:  ${this.method}
       |requestWith:  ${this.requestWith}
       |responseWith:  ${this.responseWith}
       |""".stripMargin
  }
}
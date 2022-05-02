package modux.server.lib

import akka.http.scaladsl.server.Directives.{concat, pathPrefix}
import akka.http.scaladsl.server.Route
import modux.model.ServiceEntry
import modux.model.dsl.{NameSpacedEntry, RestEntry}
import modux.model.rest.RestInstance

object RouterCapture {
  def extract(serviceEntries: Seq[ServiceEntry]): Seq[Route] = serviceEntries.flatMap {
    case NameSpacedEntry(ns, restEntry) =>
      val path: Route = concat(RouterCapture.extract(restEntry): _*)

      val s: Array[String] = {
        val v1: String = if (ns.startsWith("/")) ns.substring(1) else ns
        if (v1.endsWith("/")) v1.substring(0, v1.length - 1) else v1
      }.split("/")

      Option {
        s.reverse.foldLeft(path) { case (acc, x) => pathPrefix(x)(acc) }
      }

    case x: RestEntry =>
      x.restService match {
        case instance: RestInstance => Option(instance.route(x._extensions))
        case _ => None
      }
    case _ => None
  }
}

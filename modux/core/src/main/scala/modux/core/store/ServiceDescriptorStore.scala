package modux.core.store

import modux.core.api.Service

import scala.collection.parallel.ParIterable
import scala.collection.parallel.mutable.ParMap

case class ServiceDescriptorStore() {
  private val store: ParMap[String, Service] = ParMap.empty

  def add(name: String, service: Service): Unit = {
    store.put(name, service)
  }

  def getAll: ParIterable[Service] = store.values

  def clear(): Unit = store.clear()
}

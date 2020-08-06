package modux.model.ws

import akka.cluster.sharding.typed.scaladsl.EntityRef

trait WSConnection {
  def id: String
  def ref: EntityRef[WSCommand]

  def send(msg:String):Unit
  def close():Unit
}


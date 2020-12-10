package modux.core

import modux.core.api.Service
import modux.model.ServiceDef
import modux.model.context.Context
import modux.model.service.Call

case class SimpleTest(context: Context) extends Service {

  implicit def asAny[T](t:T): Any = t

  def test: Call[Unit, Unit] = Call.empty[Unit] {

  }

  def test2(): Call[Unit, Unit] = Call.empty[Unit] {

  }

  override def serviceDef: ServiceDef = {
    namedAs("pepe")
      .withCalls(
        get("/test", test ),
        get("/test", test2 _ ),
      )
  }
}

package example

import modux.core.api.Service
import modux.model.ServiceDef
import modux.model.header.Invoke
import modux.model.service.Call

class SimpleTest extends Service{

  val invoke:Invoke = ???
  Invoke.fail(invoke)

  def getUser(): Call[Unit, String] = onCall{

    NotFound
  }

  override def serviceDef: ServiceDef = {
    namedAs("simpleService")
      .entry(
        get("/user", getUser _)
      )
  }
}

package modux.core.example

import modux.core.api.{ModuleX, Service}
import modux.macros.serializer.SerializationSupport
import modux.macros.serializer.codec.Codec
import modux.model.ServiceDescriptor
import modux.model.context.Context
import modux.model.service.Call

case class User(name: String)

case class ErrorReport(message: String)

case class UserService(context: Context) extends Service with SerializationSupport {

  import modux.macros.serializer.SerializationSupport.DefaultCodecRegistry

  private implicit val userCodec: Codec[User] = codify[User]
  private implicit val errorCodec: Codec[ErrorReport] = codify[ErrorReport]

  private def postUser(): Call[User, Unit] = { usr =>
    println(usr)
  }

  def getUser(id: String): Call[Unit, Option[User]] = Call {

    NotFound(ErrorReport("Not found"))
  }

  override def serviceDescriptor: ServiceDescriptor = {

    namedAs("user")
      .withCalls(
        get("/user/{id}", getUser _)
          summary "Obtain's a user"
//          describe "id" as "User's  identifier" examples ("e1" -> "urn:mdx:usr:a:1111")
          response(200 summary "Returns a user" returns item[User], 400 summary "User not found" returns item[ErrorReport] sample ErrorReport("Bien!!!")),
        post("/user", postUser _)
          summary "creates a user"
          expect item[User]
          response(200 summary "Creates a user", 500 summary "Internal error" returns item[ErrorReport])
      )
  }
}

case class Mod1(context: Context) extends ModuleX {
  override def providers: Seq[Service] = Seq(
    UserService(context)
  )
}



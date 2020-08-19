package modux.core.example

import modux.core.api.{ModuleX, Service}
import modux.macros.serializer.SerializationSupport
import modux.macros.serializer.codec.Codec
import modux.model.ServiceDef
import modux.model.context.Context
import modux.model.service.Call

case class User(name: String)

case class ErrorReport(message: String)

case class UserService(context: Context) extends Service with SerializationSupport {

  import modux.macros.serializer.SerializationSupport.DefaultCodecRegistry

  private implicit val userCodec: Codec[User] = codecFor[User]
  private implicit val errorCodec: Codec[ErrorReport] = codecFor[ErrorReport]

  private def postUser(): Call[User, Unit] = { usr =>
    println(usr)
  }

  def getUser(id: String, location: String, year: Int): Call[Unit, Option[User]] = Call {

    NotFound(ErrorReport("Not found"))
  }

  override def serviceDef: ServiceDef = {

    namedAs("user")
      .withCalls(
        get("/user/:id?location&year", getUser _)
          summary "Obtains a user record using location and year"
          describe pathParam("id") as "an user id" withExamples("example1" -> "id1", "example2" -> "id2")
          describe queryParam("location") as "user location" withExamples "location1" -> "NY"
          describe queryParam("year") as "a year" withExamples "y1" -> "2020"
          returns(
          200 -> "User created" represented by[User] withExample User("Frank"),
          400 -> "User is not found" represented by[ErrorReport] withExample ErrorReport("Not founded")
        ),
        post("/user", postUser _)
          summary "creates a user"
          expects instanceOf[User]
          returns(
          200 -> "Creates a user",
          500 -> "Internal error" represented by[ErrorReport]
        )
      )
  }
}

case class Mod1(context: Context) extends ModuleX with SerializationSupport {

  override def providers: Seq[Service] = Seq(
    UserService(context)
  )
}



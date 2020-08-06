# Quick Example

Modux defines microservices using **Service** interfaces.

Lets define a basic User service.
        
```scala
trait UserService extends Service with SerializationSupport{   

implicit val userCodec: Codec[User] = codify[User]

def addUser(): Call[User, Unit]
def getUser(id: String): Call[Unit, User]

override def serviceDescriptor: ServiceDescriptor =
 namedAs("user-service")
   .withCalls(
     post("/user", addUser _),
     get("/user/{id}", getUser _)
   )

}
```


implemented by...

```scala
case class UserServiceImpl(context: Context) extends UserService{

  def addUser(): Call[User, Unit] = { user =>
    logger.info(s"user $user created")
  }

  def getUser(id: String): Call[Unit, User] = Call {
    if (math.random() < 0.5) {
      NotFound(s"User $id not found")
    } else
      User(id, ZonedDateTime.now().minusYears(10))
  }
}
```

To use this service you must register it through trait **ModuleX**. 
```scala
case class Module(context: Context) extends ModuleX {
  override def providers: Seq[Service] = Seq(
    UserService(context)
  )
}
```


Finally add to *application.conf" under "modux.modules" the classpath of Module.

> modux.modules = [ "simple.example.Module" ]

Run "sbt ~run" and invoke "http://localhost:9000/user/test".
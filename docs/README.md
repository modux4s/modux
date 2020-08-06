# Introduction :id=about

Modux is a simple and lightweight microservice server for Scala inspired on 
[Lagom](https://www.lagomframework.com/). 

It provides a easy way to define microservices 
through DSL using the power of [Scala](https://www.scala-lang.org/) and [Akka's](https://akka.io/) technologies.

# Features 

* Easy to learn and lightweight.
* Minimal configurations.
* Microservices defined by DSL.
* Open API 3.0 export based on Microservices definitions. You can extend documentations.
* Streaming.
* Websocket.
* Serialization support.
* Hot-Reloading.
* Clustering support.
* Easy production building.



# First steps


### Install

1. Add plugin `addSbtPlugin("jsoft.modux" %% "modux-plugin" % "${var.moduxVersion}")` to your **plugin.sbt**.
2. Add resolver `resolvers += Resolver.bintrayRepo("jsoft", "maven")` to your **build.sbt**.
3. Enable plugin `enablePlugins(ModuxPlugin)` in your **build.sbt**.

### Quick Example

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
case class FirstModule(context: Context) extends ModuleX {
  override def providers: Seq[Service] = Seq(
    UserService(context)
  )
}
```


Finally add to **application.conf** under **modux.modules** the classpath of  **FirstModule**.

`modux.modules = [ "simple.example.FirstModule" ]`

Run "sbt ~run" and invoke "http://localhost:9000/user/test".

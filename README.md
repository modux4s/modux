# Introduction

Modux is a simple and lightweight microservice server for Scala inspired on 
[Lagom Framework](https://www.lagomframework.com/). The aim is to speed up development using a set of directives backed by [Scala](https://www.scala-lang.org/) and [Akka's](https://akka.io/) technologies.

# Features 


* Easy to learn and lightweight.
* Minimal configurations.
* Microservices defined by DSL.
* Open API 3.0 and 2.0 exporting based on Microservices definitions. You can extend documentations.
* Streaming.
* Websocket.
* Serialization support.
* Hot-Reloading.
* Clustering support.
* Easy production building.
* Compatible with Scala 2.12

# Install

1. Add to **plugin.sbt** 
```scala
resolvers += Resolver.bintrayRepo("jsoft", "maven")
addSbtPlugin("jsoft.modux" %% "modux-plugin" % "${var.moduxVersion}")
```
2. Enable plugin `enablePlugins(ModuxPlugin)` in your **build.sbt**.

# For impatient

Run `sbt new modux4s/modux.g8` to build a Modux template project then `sbt ~run`. 

# Quick Example

The basic project structure follows a simple sbt project structure, but it is required a "conf"  folder to read "logback.xml" and "application.conf". 

File "application.conf" includes Modux modules and Akka settings.

```
root
  /conf
    application.conf
    logback.xml
  /project
  /src
  build.sbt
```

Modux defines microservices using **Service** interfaces. Lets define a basic User service.
        
```scala
trait UserService extends Service with SerializationSupport{   

  implicit val userCodec: Codec[User] = codecFor[User]

  def addUser(): Call[User, Unit]
  def getUser(id: String): Call[Unit, User]

  override def serviceDef: ServiceDef =
    namedAs("user-service")
      .withCalls(
        post("/user", addUser _),
        get("/user/:id", getUser _)
      )

}
```


implemented by...

```scala
case class UserServiceImpl(context: Context) extends UserService{

  def addUser(): Call[User, Unit] = Call{ user =>
    logger.info(s"user $user created")
  }

  def getUser(id: String): Call[Unit, User] = Call.empty {
    if (math.random() < 0.5) {
      NotFound(s"User $id not found")
    } else
      User(id, ZonedDateTime.now().minusYears(10))
  }
}
```

To use this service, it must be register through trait **ModuleX**. 

```scala
case class Module(context: Context) extends ModuleX {
  override def providers: Seq[Service] = Seq(
    UserService(context)
  )
}
```


Finally add to **/conf/application.conf** under **modux.modules** the classpath of Module.

> modux.modules = [ "simple.example.Module" ]

Run "sbt ~run" and invoke "http://localhost:9000/user/test".


# Links

* [Documentation](https://modux4s.github.io/modux)
* [Examples](https://github.com/modux4s/modux-example)


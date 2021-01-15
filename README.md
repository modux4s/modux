# Introduction

Modux is a simple and lightweight microservice server for Scala inspired on 
[Lagom Framework](https://www.lagomframework.com/). The aim is to speed up development using a set of directives backed by [Scala](https://www.scala-lang.org/) and [Akka's](https://akka.io/) technologies.

[![version](https://img.shields.io/badge/version-1.2.2-green.svg)](https://github.com/modux4s/modux)

## Features

* Easy to learn and lightweight.
* Minimal configurations.
* Microservices DSL based.
* Open API 3.0 and 2.0 exporting based on Microservices definitions. You can extend documentations.
* Streaming and Kafka support.
* GraphQL support.
* Web Security through Pac4j.
* Twirl support
* Dependency Injection through Macwire.
* Websocket.
* Serialization support.
* Hot-Reloading.
* Clustering support.
* Easy production building.
* Compatible with Scala 2.12 and 2.13.


# Install

1. Add to **plugin.sbt** 
```scala
resolvers += Resolver.bintrayRepo("jsoft", "maven")
addSbtPlugin("jsoft.modux" %% "modux-plugin" % "1.2.2")
```
2. Enable plugin `enablePlugins(ModuxPlugin)` in your **build.sbt**.

# For impatient

Run `sbt new modux4s/modux.g8` to build a Modux template project then `sbt ~run`. 

# Quick Example

The basic project structure follows next sbt project structure.
```
root
  /app
    /services
        /service1
            /impl
            Service1Def
        ...
        /serviceN
            /impl
            ServiceNDef
    /modules
        Module1
        ...
        ModuleN
    /views
        home.html.scala 
        userDetail.html.scala 
  /public
    style.css
    utils.js
    index.html
  /conf
    application.conf
    logback.xml
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
      .entry(
        post("/user", addUser _),
        get("/user/:id", getUser _)
      )
}
```

implemented by...

```scala
class UserServiceImpl extends UserService{

  def addUser(): Call[User, Unit] = extractBody{ user =>
    logger.info(s"user $user created")
  }

  def getUser(id: String): Call[Unit, User] = onCall {
    if (math.random() < 0.5) {
      NotFound(s"User $id not found")
    } else
      User(id, ZonedDateTime.now().minusYears(10))
  }
}
```

To use this service, it must be register through trait **ModuleX**.

```scala
class Module extends ModuleX {
  override def providers: Seq[Service] = Seq(wire[UserService])
}
```


Finally, add to **/conf/application.conf** under **modux.modules** the classpath of Module.

> modux.modules = [ "simple.example.Module" ]

Run "sbt ~run" and invoke "http://localhost:9000/user/test".


# Links

* [Documentation](https://modux4s.github.io/modux)
* [Examples](https://github.com/modux4s/modux-example)


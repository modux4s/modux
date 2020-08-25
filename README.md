# Introduction :id=about

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

1. Add plugin `addSbtPlugin("jsoft.modux" %% "modux-plugin" % "1.0.11")` to your **plugin.sbt**.
2. Add resolver `resolvers += Resolver.bintrayRepo("jsoft", "maven")` to your **build.sbt**.
3. Enable plugin `enablePlugins(ModuxPlugin)` in your **build.sbt**.

# Links

[Documentation](https://modux4s.github.io/modux)
[Examples](https://github.com/modux4s/modux-example)


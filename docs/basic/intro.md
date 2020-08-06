# Introduction

Modux applies some entities that helps to define a micro-services.


Someones are: 

* **Context**: This class provides a *config*, *executionContext* and *actorSystem* reference. It is useful to use Akka Actor, Clustering and Streaming.
You must provide a Context to instantiate a **Service** or a **ModuleX**.


* **ServiceDescriptor**: Defines and stores implementation and metadata about a service.

* **Service**: provides DSL to create a ServiceDescriptor. 

* **ModuleX**: group a set of **ServiceDescriptor**. It provides a **onStart** and **onStop** method. 

* **Call[IN, OUT]**: Represents a service implementation and models a flow of data. It provides a type **IN** and returns a **Future[OUT]**. Also
is possible compose then.
# Introduction :id=about

Modux is a simple and lightweight microservice server for Scala inspired on 
[Lagom Framework](https://www.lagomframework.com/). The aim is to speed up development using a set of directives backed by [Scala](https://www.scala-lang.org/) and [Akka's](https://akka.io/) technologies.

[![version](https://img.shields.io/badge/version-1.2.1-green.svg)](https://github.com/modux4s/modux)

## Features 


* Easy to learn and lightweight.
* Minimal configurations.
* Microservices defined by DSL.
* Open API 3.0 and 2.0 exporting based on Microservices definitions. You can extend documentations.
* Streaming and Kafka support.
* [GraphQL Support](https://github.com/joacovela16/graphql4s).
* Websocket.
* Serialization support.
* Hot-Reloading.
* Clustering support.
* Easy production building.
* Compatible with Scala 2.12

## Install

1. Add to **plugin.sbt** 
```scala
resolvers += Resolver.bintrayRepo("jsoft", "maven")
addSbtPlugin("jsoft.modux" %% "modux-plugin" % "${var.moduxVersion}")
```
2. Enable plugin `enablePlugins(ModuxPlugin)` in your **build.sbt**.

## For impatient

Run `sbt new modux4s/modux.g8` to build a Modux template project then `sbt ~run`. 


## Quick Example

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
      .entry(
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

# Basic

Modux applies some entities that helps to define a micro-services.


Someones are: 

* **Context**: This class provides a **config**, **executionContext** and **actorSystem** reference. It is useful to use Akka Actor, Clustering and Streaming.
You must provide a Context to instantiate a **Service** or a **ModuleX**.


* **ServiceDef**: Defines and stores implementation and metadata about a service.

* **Service**: Interface used to define a service. Provides a set of directives to create a **ServiceDef**. 

* **Call[IN, OUT]**: Represents a service implementation and models a flow of data. It provides a type **IN** and returns a **Future[OUT]**. 
Also, is possible compose then.

* **ModuleX**: group a set of **ServiceDef**. It provides a **onStart** and **onStop** method. 

## Call[IN, OUT]

Represents a flow of data where receives an input **IN** and returns a **Future[OUT]**. Requests and responses are modeled by this entity. 

```scala
// For POST call
def addUser(): Call[User, Unit] = Call{user => ??? }

def getUser(): Call[Unit, User] = Call.empty{ Future(User("Frank")) }

```

Handling request headers.

```scala
def addUser(): Call[User, Unit] = Call.withRequest{(user, request) => 
  if (!request.hasHeader("api-version")) BadRequest("Missing header")
} 

def getUser(): Call[Unit, Unit] = Call.handleRequest{ request => 
  if (!request.hasHeader("api-version")) BadRequest("Missing header")
} 
```

Composing calls ...

```scala
Call.compose{request => 
  if (request.hasHeader("someone"))
    Call{
      Future(User("Frank))
    }
  else{
    Call{BadRequest}
  }
}
```



## Service Def :id=servicedef

To build a service is required extends the Service trait. This interface provides a sets of useful directives. First, must be call the directive **nameAs** and then are available the directives  **withNamespace** and **withCalls**.

* **namedAs(serviceName)**: creates a ServiceDef named as "serviceName".
* **namespace(namespace)**: put de service under a namespace "namespace".
* **withCalls(restEntries*)**: Binds a set of RestEntry to the ServiceDef.

A **Service** provides the next directives to create a **RestEntry**:


<details >
<summary>get</summary>

Creates a RestEntry with method **GET**.

```scala
def findById(id:String): Call[Unit, User] = Call.empty{
  NotFound(s"User $id not founded")
}

get("/user/:id", findById _)
```
</details>

<details>
<summary>post</summary>

Creates a RestEntry with method **POST**.

```scala
def addUser(): Call[User, Unit] = Call{user => ???}

post("/user", addUser _)
```
</details>

<details>
<summary>delete</summary>

Creates a RestEntry with method **DELETE**.

```scala
def deleteUsers(ages: Seq[Int]): Call[Unit, Unit] = ???

delete("/user?ages", deleteUsers _)
```
</details>

<details>
<summary>head</summary>


Creates a RestEntry with method **HEAD**.

```scala
def addUser(): Call[User, Unit] = Call{user => ???}

head("/user", addUser _)
```
</details>


<details>
<summary>put</summary>


Creates a RestEntry with method **PUT**.

```scala
def addUser(): Call[User, Unit] = Call{user => ???}

put("/user", addUser _)
```
</details>

<details>
<summary>patch</summary>


Creates a RestEntry with method **PATCH**. 
```scala
def addUser(): Call[User, Unit] = Call{user => ???}

patch("/user", addUser _)
```
</details>

<details>
<summary>named</summary>


Creates a **get** if call's input is **Unit** or **NotUsed**. If call's input is an entity, creates a post.  The first argument only accepts string.

```scala
def getUser(): Call[Unit, User] = ???

named("user",getUser _)
```

```scala
def addUser(): Call[User, Unit] = ???

named("user",addUser _)
```
</details>


<details>
<summary>statics</summary>


Directive to serve static files. Useful to serve websites. Receives a prefix and directory path.

```scala
statics("/app/dashboard", "public/")
```
</details>

### Path templates 

Some of this directives (get, post, delete, head, patch and put) supports urls template. It is also possible extracts the path or query params values from a url template. 

For example: `/user/:location?age&region`

The implementation of this **must** have parameters with names "location", "age" and "region". No matter the order.

Types supported:
* Path params: **String**, **Int**, **Float**, **Double**.
* Query params: **String**, **Int**, **Float**, **Double**, **Option**, **Boolean**, **Iterable**.

<details>
<summary>Examples</summary>

```scala
def example1(location:String, age:Int, region: Seq[String]) = ???

def example2(location:Int, age:Option[Int], region: Seq[Double]) = ???

def example3(location:Int, age: Double, region: Boolean) = ???
```
</details>


### Rest Entry Extensions

A RestEntry can be extended with this directives: **[circuitBreak](https://doc.akka.io/docs/akka/current/common/circuitbreaker.html)** and **[retry](https://doc.akka.io/api/akka/current/akka/pattern/RetrySupport.html)**. It's possible define a custom extension extending **RestEntryExtension**, providing a way to work with the invocation response.

<details open>
<summary>Examples</summary>

```scala
get("/user/:id") extendedBy circuitBreak(maxFailure=1)

get("/user/:id") extendedBy circuitBreak(maxFailure=1, callTimeout = 10 s, resetTimeout = 60 s)

get("/user/:id")
  .extendedBy(
    circuitBreak(maxFailure=1, callTimeout = 10 s, resetTimeout = 60 s),
    retry(attempts = 3)
  )
```

</details>

### Response Directives

There are available some directives to response a call: **Ok**, **NotFound**, **Unauthorized**, **BadRequest**, **InternalError** and **ResponseWith**.

**NotFound**

```scala
def getUser(id:String): Call[Unit, User] = Call.empty{
  NotFound
}

def getUser(id:String): Call[Unit, User] = Call.empty{
  NotFound(s"User $id not founded")
}
```

```scala

case class ErrorReport(message:String)

case class ServiceExample(ctx: Context) extends Service with SerializationSupport{
  implicit val errorReportCodec = codecFor[ErrorReport]

  def getUser(id:String): Call[Unit, User] = Call.empty{
    NotFound(ErrorReport(s"User $id not founded"))
  }
}
```

For last example is required a Marshaller for a custom entity. [**SerializationSupport**](#serialization) provides a set of directives to build codes quickly.

Also, it is possible define customs response with directive **ResponseWith**. For example:

```scala
def addUser(): Call[User, Unit] = Call{ user => 
  ResponseWith(201, "Created")
}

def doSomething(): Call[Unit, Unit] = Call.empty{
  ResponseWith(503)
}
```

# Advanced

## Streaming :id=streaming

### Data Streaming

To receive a data streaming it is necessary define a function that returns a `Call[Source[T, Any], Unit]`. [Source](https://doc.akka.io/docs/akka/current/stream/stream-flows-and-basics.html) is an operator with exactly one output.

<details open>
<summary>Examples</summary> 

```scala

// receive a file 
def receiveFile(name: String, ext: String): Call[Source[ByteString, Any], Unit] = Call{src => 
  val file: Path = Paths.get(s"$name.$ext")
  src.runWith(FileIO.toPath(file))
}

// receive a stream of users
def receiveUsers(): Call[Source[User, Any], Unit] = ???
```
</details>

To send a streaming, must be returned a `Call[Unit, Source[T, Any]]`.

<details open>
<summary>Example</summary>

```scala

// returns a stream of users
def getUsers(): Call[Unit, Source[User, Any]] = Call.empty{
 Source(
    List[User](User("Frank"))
  )
}
```
</details>

Also it is possible "intercept" the streaming.

<details open>
<summary>Example</summary>

```scala

def getUsers(): Call[Source[Int, Any], Source[Int, Any]] = Call{ src => 
 src.map(x=> x * 2)
}
```
</details>


### Websocket

Websocket are available using the **WebSocket** constructor, which builds a `Call[WSEvent[IN, OUT], Unit]`. Messages received have type "IN" and messages sended have type "OUT". Each event are handle by **WSEvent**. To bind implementation with ServiceDef it is necessary use [**named**](#servicedef) directive.

For custom entities, like "User" it is necessary declare codecs for it. Extending trait SerializationSupport it is possible create a websocket codec.

<details open>
<summary>Example</summary>

```scala
/* Websocket example
 * To connect run: wsc http://localhost:9000/ws
 * wsc = https://www.npmjs.com/package/wsc
 */
case class SimpleExample(context: Context) extends Service with SerializationSupport{

  implicit val wsCodec: WebSocketCodec[String, User] = codecFor[String, User]

  def ws(): Call[WSEvent[String, User], Unit] = WebSocket[String, User] {
    case OnOpenConnection(connection) => 
      logger.info(s"Connection $connection created")
    case OnCloseConnection(connection) => 
      logger.info(s"Connection $connection closed")
    case OnMessage(connection, message) => 
      connection.sendMessage(User(message))
  }

  override def serviceDef: ServiceDef = {
    namedAs("User Service")
      .entry(
        named("ws", ws _)
      )
    }
}
```
</details>

## Serialization Support :id=serialization

It is possible use any library to serialize, like [Circe](https://github.com/circe/circe) or [Play](https://github.com/playframework/play-json). By default **SerializationSupport** applies [Jackson](https://github.com/FasterXML/jackson) as serializer. 

Exists 3 way to provide a codec for any entity.
1. Extending **CodecEntityProvider**. Only used in services calls, like methods POST, GET, etc.
2. Extending **CodecStreamProvider**. Used for read and write [streaming](#streaming).
3. Extending **CodecMixedProvider**. Provides codec for simple calls and streaming. Combines points 1 and 2.


By default Modux provides **CodecMixedProvider** for medias type "application/json" and "application/xml". Also provides **CodecStreamProvider** for "text/csv".


To provide customs codecs **CodecRegistry** must be extended. Modux has `SerializationSupport.DefaultCodecRegistry` which contains the defaults codecs.

```scala
// CodecRegistry example
implicit final val DefaultCodecRegistry: CodecRegistry = {
    new CodecRegistry {
      override def codecs: Seq[CodecProvider] = Seq(
          XmlCodecProvider(),
          JsonCodecProvider(),
          CsvStreamProvider()
        )
    }
  }
```

#### Directives
* **codecFor[T]**: creates a codec for entity **T**. If a custom CodecRegistry is provided, it can be call like `codecFor[T](customRegistry)`.

* **codecFor[A, B]**: Creates a codec to handle `WSEvent[A, B]` events. For now, when websocket feature is used, this directive must by applied to serialize messages. 


## Exporting APIs

A set of directives and sbt keys are available to describe microservices. See next example:

```scala

override def serviceDef: ServiceDef = 
  namedAs("user-service")
    .entry(
      get("/user/:id?location&year", getUser _)
        summary "Obtains a user record using location and year"
        describe pathParam("id") as  "an user id" withExamples ("example1" -> "id1", "example2"-> "id2")
        describe queryParam("year") as  "a year" withExamples "y1" -> "2020"
        describe queryParam("location") as  "user location" withExamples "location1" -> "NY"
        describe header("sessionID") as "session ID"
        describe cookie("lastAccess") as "last access"
        returns(
          200 -> "OK" represented by[User] withExample User("Frank"),
          400 -> "User not found" 
        ),
      post("/user", addUser _)
        summary "Creates a new user"
        expects instanceOf[User]
        returns(
          200 -> "Creates a user", 
          500 -> "Internal error" represented by[ErrorReport]
        )
    )
```

There are settings in **build.sbt** used to set global configurations in exporting.

```scala

enablePlugins(ModuxPlugin)
import modux.shared.{ServerDecl, ServerVar}

lazy val root = (project in file("."))
  .settings(
    ..... , 
    moduxOpenAPIVersion := 3, // Exports to Open API 3. Setting to 2 will export to Open API 2. Default value is version 3.
    version := "0.0.1", // project version
    description := "A Modux project example",  // project description
    contact := Some("email@email.com"), // a contact
    licenses += ("MIT", url("https://opensource.org/licenses/MIT")), // licences kind
    servers := Seq( // server hosting with variable definitions
      ServerDecl(
        url = "{schema}://localhost:{port}",
        description = "Local env",
        variables = Map(
          "schema" -> ServerVar("http", "https"),
          "port" -> ServerVar("9000", "9001"),
        )
      )
    ),
    ......
  )
```

Running `sbt moduxExportYaml` a file `{{project-name}}.yaml` under tarter/api will be created. Running `sbt moduxExportJson` will be exported in JSON format.

```yaml
openapi: 3.0.1
info:
  title: modux-test
  description: Un proyecto de ejemplo usando Modux
  license:
    name: MIT
    url: https://opensource.org/licenses/MIT
  version: "0.1"
servers:
- url: "{schema}://localhost:{port}"
  description: Ambiente local
  variables:
    schema:
      default: http
      enum:
      - http
      - https
    port:
      default: "9000"
      enum:
      - "9000"
      - "9001"
paths:
  /user:
    post:
      description: Creates a new user
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/User'
          application/xml:
            schema:
              $ref: '#/components/schemas/User'
      responses:
        "200":
          description: Creates a user
        "500":
          description: Internal error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorReport'
            application/xml:
              schema:
                $ref: '#/components/schemas/ErrorReport'
  /user/{id}:
    get:
      description: Obtains a user record using location and year
      parameters:
      - name: id
        in: path
        description: an user id
        required: true
        schema:
          type: string
        examples:
          example1:
            value: id1
          example2:
            value: id2
      - name: location
        in: query
        description: user location
        schema:
          type: string
        examples:
          location1:
            value: NY
      - name: year
        in: query
        description: a year
        schema:
          type: integer
          format: int32
        examples:
          y1:
            value: "2020"
      - name: sessionID
        in: header
        description: session ID
        schema:
          type: string
      - name: lastAccess
        in: cookie
        description: last access
        schema:
          type: string
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'
              example: "{\"name\":\"Frank\"}"
            application/xml:
              schema:
                $ref: '#/components/schemas/User'
              example: <User xmlns=""><name>Frank</name></User>
        "400":
          description: User not found
        default:
          $ref: '#/components/schemas/User'
components:
  schemas:
    User:
      type: object
      properties:
        name:
          type: string
    ErrorReport:
      type: object
      properties:
        message:
          type: string

```


## Running Modux

* Hot deploy: `sbt ~run`

* Simple run: `sbt run`

* Production build: `sbt universal:packageBin`. 

Modux uses [SbtNativePackager](https://www.scala-sbt.org/sbt-native-packager/) to packaging project. See [SbtNativePackager](https://www.scala-sbt.org/sbt-native-packager/) for more details.

## Clustering

Modux applies [Akka Clustering](https://doc.akka.io/docs/akka/current/typed/index-cluster.html) as engine. Same properties can be used to deploy a cluster. When Modux runs in develop mode, a cluster with a single node is configured. 

<details>
<summary><b>Default develop cluster settings</b></summary>

```
modux{
  modules = []
}

akka {
  loggers = ["akka.event.slf4j.Slf4jLogger"]

  loglevel = "info"
  stdout-loglevel = "off"

  actor {
    provider = "cluster"
    allow-java-serialization = on

    serializers {
      kryo = "io.altoo.akka.serialization.kryo.KryoSerializer"
    }

    serialization-bindings {
      "java.io.Serializable" = kryo
    }
  }

  http {
    server {
      websocket {
        periodic-keep-alive-max-idle = 1 second
      }
    }
  }

  remote.artery {
    canonical.port = 2553
    canonical.hostname = localhost
  }

  cluster {
    seed-nodes = [
      "akka://{{project.name}}@localhost:2553"
    ]

    sharding {
      number-of-shards = 100
    }

    downing-provider-class = "akka.cluster.sbr.SplitBrainResolverProvider"
  }
}
```
</details>

**Important**. Internally Modux creates a ActorSystem named by the **name** sbt project property. This must be taken into account when setting up a cluster. See [examples](https://www.google.com/) for details.


# Plugins

### Kafka Support

To enabled it, next steps must be followed. 

1. In your **build.sbt** add 
> enablePlugins(ModuxPlugin, KafkaSupportPlugin)  
2. Extending  **KafkaSupport** `topic` entry is added in DSL vocabulary. It receives as param
a topic name to subscribe, and an implementation with type `Topic => Future[Unit]`.


**Example**
```scala
case class DefaultService(context: Context) extends Service with KafkaSupport {

  override def serviceDef: ServiceDef = {
    namedAs("Defaultservice")
      .entry(
        topic("test", topic => println(topic))
      )
  }
}

```

#### Details

1. This feature uses [Alpakka Kafka](https://doc.akka.io/docs/alpakka-kafka/current/). 
2. Consumer settings are defined under configuration path `akka.kafka.consumer`. Server
bootstrap under `akka.kafka.bootstrap` ("localhost:9092" by default). 
   
#### Plugin settings

*KafkaSupportPlugin* auto-download and start Kafka before Modux server start. The version used it's defined 
by setting `kafkaVersion := "2.6.0"`. You can disable auto-start feature setting 
`autoLoadKafka := false`. 

### GraphQL Support



#### Quick example
```scala

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import modux.core.api.Service
import modux.core.feature.graphql.GraphQLSupport
import modux.model.ServiceDef
import modux.model.context.Context

final case class Result(message: String, code: Int)

final case class User(name: String, year: Int)

case class DefaultService(context: Context) extends Service with GraphQLSupport with LazyLogging {

  case class Query(users: () => Source[User, NotUsed], addUser: User => Result)

  val queryInstance: Query = Query(
    () => Source(List(User("Tom", (math.random() * 50).toInt))),
    user => {
      logger.info(user.toString)
      Result("User added", 200)
    }
  )

  override def serviceDef: ServiceDef = {
    namedAs("Defaultservice")
      .entry(
        graphql(
          "graphql",
          queryInstance.asQuery, 
          enableIntrospection = true,
          enableSchemaValidation = true
        )
      )
  }
}
```

```http request
POST http://localhost:9000/graphql?query={addUser($user){code}}
Accept: application/json
Content-Type: application/json

{
  "user": {
    "name": "jvc",
    "year": 2020
  }
}
```

##### Result 
```json
{
  "addUser": {
    "code": 200
  }
}
```

For more details see [GraphQL implementation](https://github.com/joacovela16/graphql4s) or 
[GraphQL Spec](https://graphql.org/).

## Need help?

Clone [Shop Example](https://github.com/modux4s/modux-example) to see more examples.

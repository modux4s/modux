Introduction
================

Modux provides a basic DSL to build a ServiceDescriptor through a Service trait. 
A service is defined extending Service trait. The directive **nameAs** must be called first, and then are availables the directives  **withNamespace** and **withCalls**.

* **namedAs(serviceName)**: creates a ServiceDescriptor named as "serviceName".
* **withNamespace(namespace)**: put de service under a namespace "namespace".
* **withCalls(restEntries*)**: Binds a set of [RestEntry](/restentry) to the ServiceDescriptor.


A **Service** provides the next directives to create a RestEntry:

* get
* post
* delete
* head
* patch
* put
* named
* statics

Some of this directives (get, post, delete, head, patch and put) supports urls template. Is possible extracts the path or query params values from a url template. 

For example: `/user/{location}?age&region`

The implementation of this **must** have parameters with names "location", "age" and "region".

Types supported:
* Path params: String, Int, Float, Double.
* Query params: String, Int, Float, Double, Option, Boolean, Iterable.

**Examples**:

```scala
def example1(location:String, age:Int, region: Seq[String]) = ???

def example2(location:Int, age:Option[Int], region: Seq[Double]) = ???

def example3(location:Int, age: Double, region: Boolean) = ???
```


#### RestEntry Directives


###### get

Creates a RestEntry with method **GET**. 
```scala
def findById(id:String): Call[Unit, User] = Call{
  NotFound(s"User $id not founded")
}

get("/user/{id}", findById _)
```
###### post

Creates a RestEntry with method **POST**. 
```scala
def addUser(): Call[User, Unit] = {user => ???}

post("/user", addUser _)
```

###### delete

Creates a RestEntry with method **DELETE**.
```scala
def deleteUsers(ages: Seq[Int]): Call[Unit, Unit] = ???

delete("/user?ages", deleteUsers _)
```

###### head

Creates a RestEntry with method **HEAD**.
```scala
def addUser(): Call[User, Unit] = {user => ???}

head("/user", addUser _)
```

###### put

Creates a RestEntry with method **PUT**.
```scala
def addUser(): Call[User, Unit] = {user => ???}

put("/user", addUser _)
```

###### patch

Creates a RestEntry with method **PATCH**. 
```scala
def addUser(): Call[User, Unit] = {user => ???}

patch("/user", addUser _)
```

###### named

Creates a **get** if call's input is **Unit** or **NotUsed**. If call's input is an entity, creates a post.   

```scala
def getUser(): Call[Unit, User] = ???

named("user",getUser _)
```

```scala
def addUser(): Call[User, Unit] = ???

named("user",addUser _)
```

###### statics
Directive to serve static files. Useful to serve websites. Receives a prefix path and directory where resources are stored.

```scala
statics("/app/dashboard", "public/")
```

#### Response Directives

There are available some directives to response a call: Ok, NotFound, Unauthorized and InternalError.

##### NotFound

```scala
def getUser(id:String): Call[Unit, User] = Call{
	NotFound
}
```

```scala
def getUser(id:String): Call[Unit, User] = Call{
	NotFound(s"User $id not founded")
}
```


```scala

case class ErrorReport(message:String)

case class ServiceExample(ctx: Context) extends Service{
	implicit val errorReportCodec: ToResponseMarshaller[ErrorReport] = ???

	def getUser(id:String): Call[Unit, User] = Call{
		NotFound(ErrorReport(s"User $id not founded"))
	}
}
```

For last example is required a Marshaller for a custom entity. 

Also it is possible define customs response with directive **Custom**. For example:

```scala
	trait ResponseExt extends ResponseDSL{
		def BadRequest[T](implicit m: ToResponseMarshaller[(Int, T)]): Future[T] = Custom(400)
		def BadRequest[T](data: T)(implicit m: ToResponseMarshaller[(Int, T)]): Future[T] = Custom(400, data)
	}
```
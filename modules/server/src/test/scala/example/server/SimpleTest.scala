package example.server

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import modux.core.api.Service
import modux.core.feature.graphql.{GraphQLInterpreter, GraphQLSupport}
import modux.core.feature.security.{SecurityComponents, SecuritySupport}
import modux.core.feature.security.model.SecureHandler
import modux.core.feature.security.storage.SessionStorage
import modux.macros.serializer.SerializationSupport
import modux.macros.serializer.codec.Codec
import modux.model.ServiceDef
import modux.model.service.Call
import org.pac4j.core.client.Clients
import org.pac4j.core.config.Config
import org.pac4j.oauth.client.TwitterClient

import scala.io.BufferedSource

case class SimpleTest() extends Service with GraphQLSupport with SerializationSupport with SecuritySupport {

  implicit val userCodec: Codec[User] = codecFor[User]

  val twitterClient = new TwitterClient("V79MSkrBk7mCCJqvEChSnSc1e", "K9FlhKRR7hB1HfdKOm9OAi1Cyu5qIDaS8pKy3xcIIHyNH8tMDH")

  val clients: Clients = new Clients("http://localhost:9000/callback", twitterClient)
  val config: Config = new Config(clients)

  case class User(name: String, year: Int)

  case class Query(users: () => User)

  val queryInstance: Query = Query(() => User("pepe", 12))

  override def securityComponents: SecurityComponents = new SecurityComponents{
    val config: Config = {
      val clients: Clients = new Clients("http://localhost:9000/callback", twitterClient)
      new Config(clients)
    }
  }

  def getUser(): Call[Unit, Source[User, NotUsed]] = Secure() {
    onCall {
      Source(List(User("pepe", 120)))
    }
  }

  def getStaticHome(id: String): Call[NotUsed, Unit] = extractContext { req =>
    println("OK")
  }

  def getStatic(basePath: String): String => Call[Unit, Source[ByteString, NotUsed]] = remaining => {
    println(remaining)
    onCall {
      mapResponse(_.asHtml) {
        Option(this.getClass.getClassLoader.getResourceAsStream(basePath + (if (remaining.isEmpty) "/index.html" else remaining))) match {
          case Some(string) =>
            val src: BufferedSource = scala.io.Source.fromInputStream(string)
            Source.fromIterator(() => src.getLines()).map(x => ByteString(x))
          case None => NotFound
        }
      }
    }
  }

  def func2(p1: String, p2: Option[String], p3: List[String]): Call[Unit, Unit] = doneWith {
    println(p1 + p2 + p3)
  }

  def func2Alt: (String, Option[String], List[String]) => Call[Unit, Unit] = (p1, p2, p3) => doneWith {
    println(p1 + p2 + p3)
  }

  val graphQLService: GraphQLInterpreter = graphql(queryInstance.asQuery)

  def secureGraphQL(): Call[Option[String], Source[ByteString, NotUsed]] = Secure(){
    graphQLService.service
  }

  override def serviceDef: ServiceDef = {

    val function: Call[Unit, Unit] = callback("/home")

    namedAs("testing")
      .entry(
        get("/callback", function),
        //        post("/callback", securityService.callback("/home")),
        //        get("/user", getUser _),
        //        call("graphql", secureGraphQL _),
        //        get("/home/:id", getStaticHome _)
      )
  }
}

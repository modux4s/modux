package example.server

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import jsoft.graphql.model.Binding
import modux.core.api.Service
import modux.core.feature.graphql.{GraphQLInterpreter, GraphQLSupport}
import modux.core.feature.security.SecuritySupport
import modux.core.feature.security.model.SecurityService
import modux.macros.serializer.SerializationSupport
import modux.macros.serializer.codec.Codec
import modux.model.ServiceDef
import modux.model.context.Context
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

  val securityService: SecurityService = security(config)

  case class User(name: String, year: Int)

  case class Query(users: () => User)

  val queryInstance: Query = Query(() => User("pepe", 12))

  def getUser(): Call[Unit, Source[User, NotUsed]] = securityService.withAuthentication() { auth =>
    onCall {
      Source(List(User("pepe", 120)))
    }
  }

  def getStaticHome(id: String): Call[NotUsed, Unit] = doneWith {
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

  def secureGraphQL(): Call[Option[String], Source[ByteString, NotUsed]] = {
    securityService.secure()(graphQLService.service)
  }

  override def serviceDef: ServiceDef = {

    namedAs("testing")
      .entry(
//        get("/callback", securityService.callback("/home")),
//        post("/callback", securityService.callback("/home")),
//        get("/user", getUser _),
//        call("graphql", secureGraphQL _),
        get("/home/:id", getStaticHome _)
      )
  }
}

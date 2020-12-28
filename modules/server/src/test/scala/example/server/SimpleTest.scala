package example.server

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import modux.core.api.Service
import modux.core.feature.graphql.GraphQLSupport
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

import java.io.{File, InputStream}

case class SimpleTest(context: Context) extends Service with GraphQLSupport with SerializationSupport with SecuritySupport {

  implicit val userCodec: Codec[User] = codecFor[User]

  val twitterClient = new TwitterClient("V79MSkrBk7mCCJqvEChSnSc1e", "K9FlhKRR7hB1HfdKOm9OAi1Cyu5qIDaS8pKy3xcIIHyNH8tMDH")

  val clients: Clients = new Clients("http://localhost:9000/callback", twitterClient)
  val config: Config = new Config(clients)

  val securityService: SecurityService = security(config)

  case class User(name: String, year: Int)

  case class Query(users: () => User)

  val queryInstance: Query = Query(() => User("pepe", 12))

  def getUser(): Call[Unit, Source[User, NotUsed]] = securityService.withAuthentication() { auth =>
    Call.empty[Source[User, NotUsed]] {
      Source(List(User("pepe", 120)))
    }
  }

  def getStaticHome(): Call[NotUsed, Unit] = Call.handleRequest { req =>
    println("OK")
  }

  def getStatic(basePath: String)(remaining: String): Call[Unit, Source[ByteString, NotUsed]] = Call.handleRequest [Unit, Source[ByteString, NotUsed]]{ req =>

    val string: InputStream = this.getClass.getClassLoader.getResourceAsStream(basePath+(if (remaining.isEmpty) "/index.html" else remaining))

//    val file = new File(string )
    val src = scala.io.Source.fromInputStream(string)
    Source.fromIterator(()=>src.getLines()).map(x=> ByteString(x))
  }.mapResponse(x=>x.asHtml)

  override def serviceDef: ServiceDef = {

    //    println(getClass.getClassLoader.getResource("./public/index.html"))
    namedAs("testing")
      .entry(
        //        get("/callback", securityService.callback("/home")),
        //        post("/callback", securityService.callback("/home")),
        //        get("/user", getUser _),
        //        statics("/home", "public"),
        namespace("/home")(
          //        get("/home", getStaticHome _),
          get("*", getStatic("public") _)
        ),
        //        graphql("graphql", queryInstance.asQuery)
      )
  }
}

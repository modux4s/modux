package modux.core

import akka.NotUsed
import akka.stream.scaladsl.Source
import modux.core.api.Service
import modux.core.feature.graphql.GraphQLSupport
import modux.macros.serializer.SerializationSupport
import modux.macros.serializer.codec.Codec
import modux.model.ServiceDef
import modux.model.context.Context
import modux.model.service.Call

case class SimpleTest(context: Context) extends Service with GraphQLSupport with SerializationSupport {

  case class User(name: String, year: Int)

  case class Query(users: () => User)

  val queryInstance: Query = Query(() => User("pepe", 12))

  implicit val userCodec: Codec[User] = codecFor[User]

  def getUser(): Call[Unit, Source[User, NotUsed]] = Call.empty[Source[User, NotUsed]] {
    Source(List(User("pepe", 120)))
  }

  override def serviceDef: ServiceDef = {

    namedAs("testing")
      .entry(
        get("/users", getUser _),
        graphql("graphql", queryInstance.asQuery)
      )
  }
}

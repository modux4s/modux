package modux.core.feature.security

import akka.NotUsed
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import modux.common.FutureImplicits
import modux.core.feature.security.SecuritySupport.{Response, WC}
import modux.core.feature.security.adapter.ModuxAdapter
import modux.core.feature.security.context.ModuxWebContext
import modux.core.feature.security.model.{AuthenticatedRequest, ResponseChanges, SecurityService}
import modux.core.feature.security.storage.{InMemory, SessionStorage}
import modux.model.header.{RequestHeader, ResponseHeader}
import modux.model.service.{Call, CallDirectives}
import org.pac4j.core.config.Config
import org.pac4j.core.engine._

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.iterableAsScalaIterableConverter

object SecuritySupport {
  type Response = Future[ResponseHeader]
  type WC = ModuxWebContext

}

trait SecuritySupport extends FutureImplicits {

  final def security(config: Config, sessionStorage: SessionStorage = InMemory(), sessionCookieName: String = ModuxWebContext.DEFAULT_COOKIE_NAME)(implicit ec: ExecutionContext): SecurityService = {

    val securityLogic: SecurityLogic[Response, WC] = Option(config.getSecurityLogic)
      .collect { case sl: SecurityLogic[Response, WC] => sl }
      .getOrElse(new DefaultSecurityLogic[Response, WC])

    val actionAdapter: ModuxAdapter = ModuxAdapter()

    val callbackLogic: CallbackLogic[Response, WC] = Option(config.getCallbackLogic)
      .collect { case cl: CallbackLogic[Response, WC] => cl }
      .getOrElse(new DefaultCallbackLogic[Response, WC])

    val logoutLogic: LogoutLogic[Response, WC] = Option(config.getLogoutLogic)
      .collect { case ll: LogoutLogic[Response, WC] => ll }
      .getOrElse(new DefaultLogoutLogic[Response, WC])

    def createContext(request: RequestHeader): ModuxWebContext = {
      ModuxWebContext(request, Nil, sessionStorage, sessionCookieName)
    }

    new SecurityService with CallDirectives {

      override def callback(
                             defaultUrl: String,
                             saveInSession: Boolean,
                             multiProfile: Boolean,
                             defaultClient: Option[String],
                             enforceFormEncoding: Boolean,
                             existingContext: Option[WC],
                             setCsrfCookie: Boolean
                           ): () => Call[Unit, NotUsed] = {
        () =>
          (_, req, res) => {

            val ctx: WC = createContext(req)
            callbackLogic
              .perform(ctx, config, actionAdapter, defaultUrl, saveInSession, multiProfile, true, defaultClient.orNull)
              .map { x =>
                (NotUsed, ResponseChanges(ctx.getChanges, x))
              }
          }
      }

      override def logout(): Call[Unit, NotUsed] = Call.handleRequest { request =>
        NotUsed
      }

      override def withAuthentication[A, B](
                                             clients: String = null,
                                             multiProfile: Boolean = true,
                                             authorizers: String = ""
                                           )(inner: AuthenticatedRequest => Call[A, B]): Call[A, B] = Call.composeAsync { request =>

        val webContext: WC = createContext(request)

        securityLogic
          .perform(
            webContext,
            config,
            (context, profiles, _: AnyRef) => {
              Future.successful(ResponseHeader.Empty)
            },
            actionAdapter,
            clients,
            authorizers,
            "",
            multiProfile
          )
          .map { auth =>
            inner(AuthenticatedRequest(webContext, Nil))
              .mapResponse { x =>
                auth //ResponseChanges(webContext.getChanges, x)
              }
          }

      }
    }
  }

}

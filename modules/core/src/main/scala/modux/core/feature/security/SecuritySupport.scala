package modux.core.feature.security

import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.util.FastFuture.EnhancedFuture
import akka.stream.Materializer
import modux.core.feature.security.SecuritySupport.{Response, WC}
import modux.core.feature.security.adapter.ModuxAdapter
import modux.core.feature.security.context.ModuxWebContext
import modux.core.feature.security.model.{AuthenticatedRequest, SecurityService}
import modux.core.feature.security.storage.{InMemory, SessionStorage}
import modux.model.header.Invoke
import modux.model.service.Call
import modux.model.service.CallDirectives._
import org.pac4j.core.config.Config
import org.pac4j.core.engine._

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters.collectionAsScalaIterableConverter

object SecuritySupport {
  type Response = Invoke
  type WC = ModuxWebContext

}

trait SecuritySupport {

  private def getFormFields(entity: HttpEntity, enforceFormEncoding: Boolean)(implicit mat: Materializer, ec: ExecutionContext): Future[Seq[(String, String)]] = {
    Unmarshal(entity)
      .to[StrictForm]
      .fast
      .flatMap { form =>
        val fields: Seq[Future[(String, String)]] = form.fields.collect {
          case (name, field) if name.nonEmpty => Unmarshal(field).to[String].map(fieldString => (name, fieldString))
        }
        Future.sequence(fields)
      }
      .recoverWith {
        case e =>
          if (enforceFormEncoding) {
            Future.failed(e)
          } else {
            Future.successful(Seq.empty)
          }
      }
  }

  private def withFormParameters[A, B](enforceFormEncoding: Boolean)(inner: Seq[(String, String)] => Call[A, B]): Call[A, B] = {
    handleContextAsync { context =>
      import context.{executionContext, materializer}
      getFormFields(context.request.entity, enforceFormEncoding).map(x => inner(x))
    }
  }

  final def security(config: Config, sessionStorage: SessionStorage = InMemory(), sessionCookieName: String = ModuxWebContext.DEFAULT_COOKIE_NAME): SecurityService = {

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

    new SecurityService {

      override def callback(
                             defaultUrl: String,
                             saveInSession: Boolean,
                             multiProfile: Boolean,
                             defaultClient: Option[String],
                             enforceFormEncoding: Boolean,
                             existingContext: Option[WC],
                             setCsrfCookie: Boolean
                           ): () => Call[Unit, Unit] = {
        () =>

          withFormParameters(enforceFormEncoding) { params =>

            handleContextAsync { context =>
              val ctx: WC = ModuxWebContext(context, params, sessionStorage, sessionCookieName)
              val inv: Invoke = callbackLogic.perform(ctx, config, actionAdapter, defaultUrl, saveInSession, multiProfile, true, defaultClient.orNull)
              if (inv.isValid) {
                Future.successful()
              } else {
                Invoke.fail(inv)
              }
            }

          }
      }

      override def logout(): Call[Unit, Unit] = handleRequest { request =>

        ???
      }

      override def withAuthentication[A, B](
                                             clients: String = null,
                                             multiProfile: Boolean = true,
                                             authorizers: String = ""
                                           )(inner: AuthenticatedRequest => Call[A, B]): Call[A, B] = handleContext { context =>

        val webContext: WC = ModuxWebContext(context, Nil, sessionStorage, sessionCookieName)

        mapContextAsync { _ =>
          val inv = securityLogic
            .perform(
              webContext,
              config,
              (context, profiles, params: AnyRef) => {
                context.invoke.withProfiles(profiles.asScala.toList)
              },
              actionAdapter,
              clients,
              authorizers,
              "",
              multiProfile
            )

          if (inv.isValid) {
            Future.successful(inv)
          } else {
            Invoke.fail(inv)
          }
        } {
          (in, invoke) => {
            inner(AuthenticatedRequest(webContext, invoke.profiles))(in, invoke)
          }
        }
      }
    }
  }

}

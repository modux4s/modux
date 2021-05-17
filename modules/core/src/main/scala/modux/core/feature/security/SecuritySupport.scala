package modux.core.feature.security

import akka.http.scaladsl.common.StrictForm
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.util.FastFuture.EnhancedFuture
import akka.stream.Materializer
import modux.core.feature.security.SecuritySupport.{Response, WC}
import modux.core.feature.security.adapter.ModuxAdapter
import modux.core.feature.security.context.ModuxWebContext
import modux.core.feature.security.storage.SessionStorage
import modux.model.directives.CallDirectives
import modux.model.header.Invoke
import modux.model.service.Call
import org.pac4j.core.config.Config
import org.pac4j.core.engine._
import org.pac4j.core.util.Pac4jConstants

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

object SecuritySupport {
  type Response = Invoke
  type WC = ModuxWebContext

}

trait SecuritySupport extends CallDirectives {

  private final val config: Config = securityComponents.config
  private final val sessionStorage: SessionStorage = securityComponents.sessionStorage
  private final val sessionCookieName: String = securityComponents.sessionCookieName
  private final val securityLogic: SecurityLogic[Response, WC] = Option(config.getSecurityLogic)
    .collect { case sl: SecurityLogic[Response, WC] => sl }
    .getOrElse(new DefaultSecurityLogic[Response, WC])

  private final val actionAdapter: ModuxAdapter = ModuxAdapter()

  private final val callbackLogic: CallbackLogic[Response, WC] = Option(config.getCallbackLogic)
    .collect { case cl: CallbackLogic[Response, WC] => cl }
    .getOrElse(new DefaultCallbackLogic[Response, WC])

  private final val logoutLogic: LogoutLogic[Response, WC] = Option(config.getLogoutLogic)
    .collect { case ll: LogoutLogic[Response, WC] => ll }
    .getOrElse(new DefaultLogoutLogic[Response, WC])

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
    extractContextAsync { context =>
      import context.{executionContext, materializer}
      getFormFields(context.request.entity, enforceFormEncoding).map(x => inner(x))
    }
  }

  def securityComponents: SecurityComponents

  final def Secure[in, out](
                             clients: String = null,
                             multiProfile: Boolean = true,
                             authorizers: String = "",
                             enforceFormEncoding: Boolean = false
                           )(inner: Call[in, out]): Call[in, out] = {

    withFormParameters[in, out](enforceFormEncoding) { params =>
      extractContext { context =>

        val webContext: WC = ModuxWebContext(context, params, sessionStorage, sessionCookieName)

        mapContextAsync { _ =>
          val inv: Response = securityLogic
            .perform(
              webContext,
              config,
              (context, profiles, _: AnyRef) => {
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
            inner(in, invoke)
          }
        }
      }
    }

  }

  final def SecureCallback(
                      defaultUrl: String,
                      saveInSession: Boolean = true,
                      multiProfile: Boolean = true,
                      defaultClient: Option[String] = None,
                      enforceFormEncoding: Boolean = false
                    ): Call[Unit, Unit] = {
    withFormParameters(enforceFormEncoding) { params =>

      mapContextAsync { context =>
        val ctx: WC = ModuxWebContext(context, params, sessionStorage, sessionCookieName)
        val inv: Invoke = callbackLogic.perform(ctx, config, actionAdapter, defaultUrl, saveInSession, multiProfile, true, defaultClient.orNull)
        if (inv.isValid) {
          Future.successful(inv)
        } else {
          Invoke.fail(inv)
        }
      } {
        (_, _) => Future.successful()
      }
    }
  }

  final def SecureLogout(
                    defaultUrl: String = Pac4jConstants.DEFAULT_URL_VALUE,
                    logoutPatternUrl: String = Pac4jConstants.DEFAULT_LOGOUT_URL_PATTERN_VALUE,
                    localLogout: Boolean = true,
                    destroySession: Boolean = true,
                    centralLogout: Boolean = false
                  ): Call[Unit, Unit] = {
    extractContextAsync { context =>
      val webContext: WC = ModuxWebContext(context, Nil, sessionStorage, sessionCookieName)
      val inv: Response = logoutLogic.perform(
        webContext,
        config,
        actionAdapter,
        defaultUrl,
        logoutPatternUrl,
        localLogout,
        destroySession,
        centralLogout
      )

      if (inv.isValid) {
        Future.successful()
      } else {
        Invoke.fail(inv)
      }
    }
  }

  /*
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
                             ): Call[Unit, Unit] = {

          withFormParameters(enforceFormEncoding) { params =>

            mapContextAsync { context =>
              val ctx: WC = ModuxWebContext(context, params, sessionStorage, sessionCookieName)
              val inv: Invoke = callbackLogic.perform(ctx, config, actionAdapter, defaultUrl, saveInSession, multiProfile, true, defaultClient.orNull)
              if (inv.isValid) {
                Future.successful(inv)
              } else {
                Invoke.fail(inv)
              }
            } {
              (_, _) => Future.successful()
            }
          }
        }

        override def logout(
                             defaultUrl: String,
                             logoutPatternUrl: String,
                             localLogout: Boolean,
                             destroySession: Boolean,
                             centralLogout: Boolean
                           ): Call[Unit, Unit] = extractContextAsync { context =>

          val webContext: WC = ModuxWebContext(context, Nil, sessionStorage, sessionCookieName)
          val inv: Response = logoutLogic.perform(
            webContext,
            config,
            actionAdapter,
            defaultUrl,
            logoutPatternUrl,
            localLogout,
            destroySession,
            centralLogout
          )

          if (inv.isValid) {
            Future.successful()
          } else {
            Invoke.fail(inv)
          }
        }

        override def extractProfiles: SecureHandler = {
          new SecureHandler with CallDirectives {
            override def apply[in, out](inner: => Call[in, out]): Call[in, out] = mapContext { context =>
              val webContext: WC = ModuxWebContext(context, Nil, sessionStorage, sessionCookieName)
              val profileManager = new ProfileManager[UserProfile](webContext)
              context.withProfiles(profileManager.getAllLikeDefaultSecurityLogic(true).asScala.toList)
            }(inner)
          }
        }

        override def secureWithAuth(
                                     clients: String = null,
                                     multiProfile: Boolean = true,
                                     authorizers: String = ""
                                   ): SecureHandlerWithAuth = {

          new SecureHandlerWithAuth {
            override def apply[in, out](inner: AuthenticatedRequest => Call[in, out]): Call[in, out] = {
              withFormParameters(false) { params =>
                extractContext { context =>

                  val webContext: WC = ModuxWebContext(context, params, sessionStorage, sessionCookieName)

                  mapContextAsync { _ =>
                    val inv: Response = securityLogic
                      .perform(
                        webContext,
                        config,
                        (context, profiles, _: AnyRef) => {
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
        }
      }
    }
  */

}

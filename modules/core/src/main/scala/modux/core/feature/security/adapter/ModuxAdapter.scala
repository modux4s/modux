package modux.core.feature.security.adapter

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import modux.core.feature.security.SecuritySupport.Response
import modux.core.feature.security.context.ModuxWebContext
import modux.model.header.ResponseHeader
import org.pac4j.core.context.HttpConstants
import org.pac4j.core.exception.http._
import org.pac4j.core.http.adapter.HttpActionAdapter

final case class ModuxAdapter() extends HttpActionAdapter[Response, ModuxWebContext] {

  override def adapt(action: HttpAction, context: ModuxWebContext): Response = {
    val invoke: Response = context.invoke
    val header: ResponseHeader = invoke.responseHeader

    action match {
      case _: UnauthorizedAction =>
        // XHR requests don't receive a TEMP_REDIRECT but a UNAUTHORIZED. The client can handle this
        // to trigger the proper redirect anyway, but for a correct flow the session cookie must be set
        header
          .withCookie(context.getResponseSessionCookie)
          .withStatus(Unauthorized.intValue)

      case _: BadRequestAction =>
        header.withStatus(BadRequest.intValue)
      case _ if action.getCode == HttpConstants.CREATED =>
        header.withStatus(Created.intValue)
      case _: ForbiddenAction =>
        header.withStatus(Forbidden.intValue)
      case a: FoundAction =>
        header
          .withStatus(SeeOther.intValue)
          .withHeaders(List(Location(Uri(a.getLocation))))
          .withCookie(context.getResponseSessionCookie)

      case a: SeeOtherAction =>
        header
          .withStatus(SeeOther.intValue)
          .withHeaders(List(Location(Uri(a.getLocation))))
          .withCookie(context.getResponseSessionCookie)
      case a: OkAction =>
        //        val contentBytes: Array[Byte] = a.getContent.getBytes
        //        val entity: HttpEntity.Strict = context.getContentType.map(ct => HttpEntity(ct, contentBytes)).getOrElse(HttpEntity(contentBytes))
        header.withStatus(OK.intValue)
      case _: NoContentAction =>
        header.withStatus(NoContent.intValue)
      case _ if action.getCode == 500 =>
        header.withStatus(InternalServerError.intValue)
      case _ =>
        header.withStatus(StatusCodes.getForKey(action.getCode).getOrElse(custom(action.getCode, "")).intValue())
    }

    invoke
  }

}

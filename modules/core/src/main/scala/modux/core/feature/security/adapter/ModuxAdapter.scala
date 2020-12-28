package modux.core.feature.security.adapter

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Location
import modux.core.feature.security.SecuritySupport
import modux.core.feature.security.context.ModuxWebContext
import modux.model.header.ResponseHeader
import org.pac4j.core.context.HttpConstants
import org.pac4j.core.exception.http._
import org.pac4j.core.http.adapter.HttpActionAdapter

import scala.concurrent.Future

final case class ModuxAdapter() extends HttpActionAdapter[SecuritySupport.Response, ModuxWebContext] {

  override def adapt(action: HttpAction, context: ModuxWebContext): SecuritySupport.Response = Future.successful {

    action match {
      case _: UnauthorizedAction =>
        // XHR requests don't receive a TEMP_REDIRECT but a UNAUTHORIZED. The client can handle this
        // to trigger the proper redirect anyway, but for a correct flow the session cookie must be set
        context.addResponseSessionCookie()
        ResponseHeader(Unauthorized.intValue)
      case _: BadRequestAction =>
        ResponseHeader(BadRequest.intValue)
      case _ if action.getCode == HttpConstants.CREATED =>
        ResponseHeader(Created.intValue)
      case _: ForbiddenAction =>
        ResponseHeader(Forbidden.intValue)
      case a: FoundAction =>
        context.addResponseSessionCookie()
        ResponseHeader(SeeOther.intValue, headers = List[HttpHeader](Location(Uri(a.getLocation))))
      case a: SeeOtherAction =>
        context.addResponseSessionCookie()
        ResponseHeader(SeeOther.intValue, headers = List[HttpHeader](Location(Uri(a.getLocation))))
      case a: OkAction =>
        //        val contentBytes: Array[Byte] = a.getContent.getBytes
        //        val entity: HttpEntity.Strict = context.getContentType.map(ct => HttpEntity(ct, contentBytes)).getOrElse(HttpEntity(contentBytes))
        ResponseHeader(OK.intValue)
      case _: NoContentAction =>
        ResponseHeader(NoContent.intValue)
      case _ if action.getCode == 500 =>
        ResponseHeader(InternalServerError.intValue)
      case _ =>
        ResponseHeader(StatusCodes.getForKey(action.getCode).getOrElse(custom(action.getCode, "")).intValue())
    }

  }

}

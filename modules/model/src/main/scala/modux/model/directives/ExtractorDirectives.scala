package modux.model.directives

import akka.http.scaladsl.model.{HttpMethod, HttpRequest}
import modux.model.header.{Invoke, ResponseHeader}
import modux.model.service.Call
import org.pac4j.core.profile.UserProfile

import scala.concurrent.Future

trait ExtractorDirectives {

	implicit final def asDone[IN, OUT](datum: => OUT): Call[IN, OUT] = (_, _) => Future.successful(datum)

	implicit final def asCall[IN, OUT](datum: => Future[OUT]): Call[IN, OUT] = (_, _) => datum

	final def mapContextAsync[IN, OUT](mapper: Invoke => Future[Invoke])(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, req) => {
		mapper(req).flatMap(x => inner(in, x))(req.executionContext)
	}

	final def mapContext[IN, OUT](mapper: Invoke => Invoke)(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, req) => {
		inner(in, mapper(req))
	}

	final def extractContextAsync[IN, OUT](mapper: Invoke => Future[Call[IN, OUT]]): Call[IN, OUT] = (in, invoke) => {
		mapper(invoke).flatMap(x => x(in, invoke))(invoke.executionContext)
	}

	final def extractContext[IN, OUT](mapper: Invoke => Call[IN, OUT]): Call[IN, OUT] = (in, inv) => {
		mapper(inv)(in, inv)
	}

	final def extractProfiles[IN, OUT](mapper: Seq[UserProfile] => Call[IN, OUT]): Call[IN, OUT] = (in, inv) => {
		mapper(inv.profiles)(in, inv)
	}

	final def extractRequest[IN, OUT](mapper: HttpRequest => Call[IN, OUT]): Call[IN, OUT] = (in, inv) => {
		extractContext { ctx => mapper(ctx.request) }(in, inv)
	}

	final def mapResponseAsync[IN, OUT](mapper: ResponseHeader => Future[ResponseHeader])(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, inv) => {
		mapper(inv.responseHeader).flatMap(resMod => inner(in, inv.withResponseHeader(resMod)))(inv.executionContext)
	}

	final def mapResponse[IN, OUT](mapper: ResponseHeader => ResponseHeader)(inner: => Call[IN, OUT]): Call[IN, OUT] = (in, req) => {
		inner(in, req.mapResponse(mapper))
	}

	final def extractMethod[IN, OUT](handler: HttpMethod => Call[IN, OUT]): Call[IN, OUT] = extractRequest(req => handler(req.method))

	final def extractBody[IN, OUT](mapper: IN => Call[IN, OUT]): Call[IN, OUT] = (in, req) => {
		mapper(in)(in, req)
	}

	final def onCall[IN, OUT](f: => Call[IN, OUT]): Call[IN, OUT] = (in, req) => {
		f(in, req)
	}
}

package modux.model.directives

import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}
import modux.model.header.Invoke
import modux.model.service.Call

import scala.concurrent.ExecutionContextExecutor

trait ResponseDirectives {

  private def sendWithCode[IN, OUT](code: Int): Call[IN, OUT] = (_, ctx) => {
    Invoke.fail(ctx.mapResponse(_.withStatus(code)))
  }

  private def sendWithData[A, IN, OUT](code: Int, data: A)(implicit m: ToResponseMarshaller[A]): Call[IN, OUT] = (_, invoke) => {
    implicit val ec: ExecutionContextExecutor = invoke.requestContext.executionContext
    val t: ToResponseMarshallable = ToResponseMarshallable(data)

    t(invoke.request).flatMap(r => Invoke.fail(invoke.mapResponse(_.withStatus(code)), r))
  }

  final def Ok[IN, OUT](d: OUT): Call[IN, OUT] = CallDirectives.asDone(d)

  final def NotFound[IN, OUT]: Call[IN, OUT] = sendWithCode(404)

  final def NotFound[A, IN, OUT](d: A)(implicit m: ToResponseMarshaller[A]): Call[IN, OUT] = sendWithData(404, d)

  final def Redirect[IN, OUT](url: String, code: Int): Call[IN, OUT] = (_, ctx) => {
    Invoke.fail(ctx.mapResponse(_.withStatus(code).withHeader("Location", url)))
  }

  final def BadRequest[IN, OUT]: Call[IN, OUT] = sendWithCode(400)

  final def BadRequest[A, IN, OUT](d: A)(implicit m: ToResponseMarshaller[A]): Call[IN, OUT] = sendWithData(400, d)

  final def Unauthorized[IN, OUT]: Call[IN, OUT] = sendWithCode(401)

  final def Unauthorized[A, IN, OUT](d: A)(implicit m: ToResponseMarshaller[A]): Call[IN, OUT] = sendWithData(401, d)

  final def InternalError[IN, OUT]: Call[IN, OUT] = sendWithCode(500)

  final def InternalError[A, IN, OUT](d: A)(implicit m: ToResponseMarshaller[A]): Call[IN, OUT] = sendWithData(500, d)

  final def ResponseWith[IN, OUT](code: Int): Call[IN, OUT] = sendWithCode(code)

  final def ResponseWith[A, IN, OUT](code: Int, d: A)(implicit m: ToResponseMarshaller[A]): Call[IN, OUT] = sendWithData(code, d)
}
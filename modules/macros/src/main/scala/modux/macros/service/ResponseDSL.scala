package modux.macros.service

import scala.concurrent.{ExecutionContext, Future}

trait ResponseDSL {

  def Ok[A](d: A)(implicit ec: ExecutionContext): Future[A] = Future(d)

  def NotFound: Nothing = macro ResponseSupportMacro.NotFoundEmpty
  def NotFound[A](d: A): Nothing = macro ResponseSupportMacro.NotFound[A]

  def BadRequest: Nothing = macro ResponseSupportMacro.BadRequestEmpty
  def BadRequest[A](d: A): Nothing = macro ResponseSupportMacro.BadRequest[A]

  def Unauthorized: Nothing = macro ResponseSupportMacro.UnauthorizedEmpty
  def Unauthorized[A](d: A): Nothing = macro ResponseSupportMacro.Unauthorized[A]

  def InternalError: Nothing = macro ResponseSupportMacro.InternalErrorEmpty
  def InternalError[A](d: A): Nothing = macro ResponseSupportMacro.InternalError[A]

  def ResponseWith(code:Int): Nothing = macro ResponseSupportMacro.responseWithEmpty
  def ResponseWith[A](code:Int, d:A): Nothing = macro ResponseSupportMacro.responseWith[A]
}

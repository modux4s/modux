package modux.model.dsl

sealed trait ParamKind  {
  def name:String
}

final case class PathKind(name:String) extends ParamKind
final case class CookieKind(name:String) extends ParamKind
final case class HeaderKind(name:String) extends ParamKind
final case class QueryKind(name:String) extends ParamKind

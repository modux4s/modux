package modux.core.support

import modux.macros.service.ResponseDSL

trait ResponseExtensions extends ResponseDSL{

   def Redirect(url: String): Nothing = Redirect(url, 307)

}

package modux.macros.serializer.twirl

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{MediaType, MediaTypes}
import play.twirl.api.Content

trait TwirlSerialization {
  /** Serialize Twirl `Html` to `text/html`. */
  private val twirlHtmlMarshaller: ToEntityMarshaller[Content] = twirlMarshaller(MediaTypes.`text/html`)

  /** Serialize Twirl `Txt` to `text/plain`. */
  private val twirlTxtMarshaller: ToEntityMarshaller[Content] = twirlMarshaller(MediaTypes.`text/plain`)
  private val twirlJsonMarshaller: ToEntityMarshaller[Content] = twirlMarshaller(MediaTypes.`application/json`)
  private val twirlCSVMarshaller: ToEntityMarshaller[Content] = twirlMarshaller(MediaTypes.`text/csv`)

  /** Serialize Twirl `Xml` to `text/xml`. */
  private val twirlXmlMarshaller: ToEntityMarshaller[Content] = twirlMarshaller(MediaTypes.`text/xml`)

  private val twirlJavaScriptMarshaller: ToEntityMarshaller[Content] = twirlMarshaller(MediaTypes.`application/javascript`)

  /** Serialize Twirl formats to `String`. */
  private def twirlMarshaller(contentType: MediaType): ToEntityMarshaller[Content] = {

    Marshaller.StringMarshaller.wrap(contentType)(_.toString)
  }

  protected implicit def asTwirl[T <: Content]: ToEntityMarshaller[Content] = {
    Marshaller.oneOf(
      twirlHtmlMarshaller,
      twirlTxtMarshaller,
      twirlXmlMarshaller,
      twirlJsonMarshaller,
      twirlCSVMarshaller,
      twirlJavaScriptMarshaller
    )
  }

}

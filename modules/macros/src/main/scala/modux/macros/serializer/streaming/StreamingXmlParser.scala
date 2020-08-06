package modux.macros.serializer.streaming

import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.util.ByteString
import com.fasterxml.aalto.{AsyncByteArrayFeeder, AsyncXMLInputFactory, AsyncXMLStreamReader}
import com.fasterxml.aalto.stax.InputFactoryImpl
import javax.xml.stream.XMLStreamConstants

class StreamingXmlParser(maximumTextLength: Int = 1024) extends GraphStage[FlowShape[ByteString, ByteString]] {
  private val in: Inlet[ByteString] = Inlet("XMLParser.in")
  private val out: Outlet[ByteString] = Outlet("XMLParser.out")

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with InHandler with OutHandler {
    private val feeder: AsyncXMLInputFactory = new InputFactoryImpl()
    private val parser: AsyncXMLStreamReader[AsyncByteArrayFeeder] = feeder.createAsyncFor(Array.empty)
    private val buffer: StringBuilder = new StringBuilder(maximumTextLength)

    override def onPull(): Unit = {
      callParser()
    }

    override def onPush(): Unit = {
      val arr: Array[Byte] = grab(in).toArray
      parser.getInputFeeder.feedInput(arr, 0, arr.length)
    }

    override def onUpstreamFinish(): Unit = {
      parser.getInputFeeder.endOfInput()
      if (!parser.hasNext) completeStage()
      else if (isAvailable(out)) callParser()
    }

    private def checkBufferSize(): Unit = {
      if ( /*parser.getTextLength +*/ buffer.length > maximumTextLength) {
        failStage(new IllegalStateException(s"Too long character sequence"))
      }
    }

    private def appendStartNode(): Unit = {

      checkBufferSize()

      val localName: String = parser.getLocalName
      val attr: String = (0 until parser.getAttributeCount).map(i => s"""${parser.getAttributeName(i)}="${parser.getAttributeValue(i)}"""").mkString("")
      val value: String = Seq(localName, attr).filterNot(_.isEmpty).mkString("<", " ", ">")

      buffer.append(value)
    }

    private def appendEndNode(): Unit = {
      checkBufferSize()
      val localName: String = parser.getLocalName
      buffer.append(s"</$localName>")
    }

    private def appendText(): Unit = {
      checkBufferSize()
      buffer.append(parser.getText())
      callParser()
    }

    @scala.annotation.tailrec
    private def callParser(): Unit = {
      if (parser.hasNext) {
        parser.next() match {

          case AsyncXMLStreamReader.EVENT_INCOMPLETE =>

            if (!isClosed(in)) {
              pull(in)
            } else failStage(
              new IllegalStateException("Stream finished early.")
            )

          case XMLStreamConstants.SPACE | XMLStreamConstants.COMMENT | XMLStreamConstants.CDATA => callParser()
          case XMLStreamConstants.CHARACTERS => appendText()
          case XMLStreamConstants.START_ELEMENT =>

            if (parser.getDepth > 1) {
              appendStartNode()
            }

            callParser()

          case XMLStreamConstants.END_ELEMENT =>

            val depth: Int = parser.getDepth
            appendEndNode()

            if (depth == 2) {
              val value: String = buffer.toString()
              buffer.clear()
              push(out, ByteString(value))
            } else {
              callParser()
            }

          case _ => callParser()
        }
      } else {
        completeStage()
      }
    }

    setHandlers(in, out, this)
  }

  override def shape: FlowShape[ByteString, ByteString] = FlowShape(in, out)
}

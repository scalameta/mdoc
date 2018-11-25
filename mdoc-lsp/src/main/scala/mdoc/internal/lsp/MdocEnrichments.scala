package mdoc.internal.lsp

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import mdoc.internal.document.MdocExceptions
import scala.meta.inputs.Input
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath

object MdocEnrichments {
  implicit class XtensionMdocThrowable(e: Throwable) {
    def formattedStackTrace: String = {
      MdocExceptions.trimStacktrace(e)
      val msg = new ByteArrayOutputStream()
      e.printStackTrace(new PrintStream(msg))
      msg.toString(StandardCharsets.UTF_8.name())
    }
  }
  implicit class XtensionMdocUri(uri: String) {
    def toInput: Input.VirtualFile = {
      val text = FileIO.slurp(toAbsolutePath, StandardCharsets.UTF_8)
      Input.VirtualFile(uri, text)
    }
    def toAbsolutePath: AbsolutePath = {
      AbsolutePath(Paths.get(URI.create(uri)))
    }
  }
}

package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import mdoc.Variable

class ReplVariablePrinter(
    leadingNewline: Boolean,
    width: Int,
    height: Int,
    indent: Int
) extends (mdoc.Variable => String) {

  override def apply(binder: Variable): String = {
    if (binder.isUnit) ""
    else {
      val baos = new ByteArrayOutputStream()
      val sb = new PrintStream(baos)
      if (leadingNewline) {
        sb.append('\n')
      }
      val lines = pprint.PPrinter.BlackWhite.tokenize(
        binder.runtimeValue,
        width = width,
        height = height,
        indent = 2
      )
      sb.append("// ")
        .append(binder.name)
        .append(": ")
        .append(binder.staticType)
        .append(" = ")
      lines.foreach { lineStr =>
        val line = lineStr.plainText
        Renderer.appendMultiline(sb, line)
      }
      baos.toString()
    }
  }
}
object ReplVariablePrinter
    extends ReplVariablePrinter(
      leadingNewline = true,
      width = 80,
      height = 50,
      indent = 2
    )

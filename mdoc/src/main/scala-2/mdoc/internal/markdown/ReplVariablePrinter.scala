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
      sb.append("// ")
        .append(binder.name)
        .append(": ")
        .append(binder.staticType)
        .append(" = ")
      if (binder.isToString) {
        Renderer.appendMultiline(sb, binder.runtimeValue.toString)
      } else {
        val heightOverride = binder.mods.heightOverride
        val widthOverride = binder.mods.widthOverride

        val lines = pprint.PPrinter.BlackWhite.tokenize(
          binder.runtimeValue,
          width = widthOverride.getOrElse(width),
          height = heightOverride.getOrElse(height),
          indent = 2,
          initialOffset = baos.size()
        )
        lines.foreach { lineStr =>
          val line = lineStr.plainText
          Renderer.appendMultiline(sb, line)
        }
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

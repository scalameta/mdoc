package mdoc.internal.markdown

import java.io.PrintStream

class Nesting(sb: PrintStream) {
  private var nestCount = 0
  def nest(): Unit = {
    nestCount += 1
    sb.append(s"_root_.scala.Predef.locally {\n")
  }
  def unnest(): Unit = {
    1.to(nestCount).foreach { _ =>
      sb.print("};")
    }
    nestCount = 0
  }
}

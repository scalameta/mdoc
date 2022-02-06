package mdoc.internal

package object markdown {

  private[markdown] implicit class StringOps(private val x: String) extends AnyVal {

    def isEmpty: Boolean = x.forall { c => c == '\n' || c == '\r' }

    def escaped: String = x.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
  }

  private[markdown] implicit class StringBuilderOps(private val x: StringBuilder) extends AnyVal {

    def appendLinesPrefixed(prefix: String, text: String): Unit = {
      text.linesWithSeparators foreach { line =>
        if (line.nonEmpty && !line.isNL && !line.startsWith(prefix)) x.append(prefix)
        x.append(line)
      }
    }
  }
}

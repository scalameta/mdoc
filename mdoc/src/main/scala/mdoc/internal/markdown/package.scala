package mdoc.internal

package object markdown {

  private[markdown] implicit class StringOps(private val x: String) extends AnyVal {

    def isNL: Boolean = x.forall { c => c == '\n' || c == '\r' }

    def escaped: String = x.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
  }

  private[markdown] implicit class StringBuilderOps(private val x: StringBuilder) extends AnyVal {

    def appendLinesPrefixed(prefix: String, text: String): Unit = {
      text.linesWithSeparators.zipWithIndex foreach { case (line, i) =>
        if (line.nonEmpty && !line.isNL && !line.startsWith(prefix)) x.append(prefix)
        x.append(line)
      }
    }
  }
}

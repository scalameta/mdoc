package mdoc.internal.worksheets

import scala.tools.nsc.Global

object Compat {
  def usedDummy() = () // Only here to avoid "unused import" warning.
  implicit class XtensionCompiler(compiler: Global) {
    def close(): Unit = () // do nothing, not available
  }
}

package mdoc.modifiers

import scala.reflect.io.VirtualDirectory
import mdoc.internal.markdown.MarkdownCompiler
import scala.meta.inputs.Input
import mdoc.Reporter
import mdoc.internal.pos.TokenEditDistance
import mdoc.internal.markdown.FileImport

private[modifiers] object CompilerCompat {
  def abstractFile(tg: String) = new VirtualDirectory(tg, None)
}

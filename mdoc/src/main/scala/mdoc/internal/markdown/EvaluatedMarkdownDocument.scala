package mdoc.internal.markdown

import java.{util => ju}
import mdoc.{interfaces => i}
import java.nio.file.Path

case class EvaluatedMarkdownDocument(
    val diagnostics: ju.List[i.Diagnostic],
    val content: String
) extends mdoc.interfaces.EvaluatedMarkdownDocument

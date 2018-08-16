package vork.internal.markdown

import scala.meta.inputs.Input
import vork.document.Document
import vork.internal.pos.TokenEditDistance

case class EvaluatedDocument(
    instrumented: Input,
    edit: TokenEditDistance,
    sections: List[EvaluatedSection]
)

object EvaluatedDocument {
  def apply(document: Document, trees: List[SectionInput]): EvaluatedDocument = {
    val instrumented =
      Input.VirtualFile(document.instrumented.filename, document.instrumented.text)
    val edit = TokenEditDistance.toTokenEdit(trees.map(_.source), instrumented)
    EvaluatedDocument(
      instrumented,
      edit,
      document.sections.zip(trees).map {
        case (a, b) => EvaluatedSection(a, b.input, b.source, b.mod)
      }
    )
  }
}

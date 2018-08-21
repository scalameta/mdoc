package mdoc.internal.markdown

import scala.meta.Source
import scala.meta.inputs.Input
import mdoc.document.Section

case class EvaluatedSection(section: Section, input: Input, source: Source, mod: Modifier) {
  def out: String = section.statements.map(_.out).mkString
}

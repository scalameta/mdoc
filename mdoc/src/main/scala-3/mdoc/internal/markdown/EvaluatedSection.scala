package mdoc.internal.markdown

import scala.meta.Source
import scala.meta.inputs.Input
import mdoc.document.Section
import dotty.tools.dotc.ast.untpd.Tree

// Can be removed once the project is updated to use Scalameta parser for Scala 3 
case class EvaluatedSection(section: Section, input: Input, source: Tree, mod: Modifier) {
  def out: String = section.statements.map(_.out).mkString
}

package mdoc.internal.markdown

import scala.meta.Position
import scala.meta.Tree
import scala.meta.Source
import scala.meta.inputs.Input
import mdoc.document.Section

case class ParsedSource(source: Tree, stats: List[Tree]) {
  def pos: Position = source.pos
}

object ParsedSource {
  def apply(source: Source): ParsedSource = ParsedSource(source, source.stats)
  def empty = Source(Nil)
}

package mdoc.internal.markdown

import dotty.tools.dotc.ast.untpd.Tree
import dotty.tools.dotc.ast.untpd
import scala.meta.Position
import mdoc.internal.pos.TokenEditDistance
import dotty.tools.dotc.interfaces.SourcePosition
import mdoc.internal.pos.PositionSyntax._

case class ParsedSource(tree: Tree, sourcePos: SourcePosition, edit: TokenEditDistance, stats: List[ParsedSource]) {
  // TODO: figure out how to 
  def pos: Position = ParsedSource.toMetaPosition(edit, sourcePos)
}

object ParsedSource {
  def empty = untpd.EmptyTree
  def toMetaPosition(
      edit: TokenEditDistance,
      position: SourcePosition
  ): Position = {
    def toOffsetPosition(offset: Int): Position = {
      edit.toOriginal(offset) match {
        case Left(_) =>
          Position.None
        case Right(p) =>
          p.toUnslicedPosition
      }
    }
    (edit.toOriginal(position.start), edit.toOriginal(position.end - 1)) match {
      case (Right(start), Right(end)) =>
        Position.Range(start.input, start.start, end.end).toUnslicedPosition
      case (_, _) =>
        toOffsetPosition(position.point - 1)
    }
  }
}
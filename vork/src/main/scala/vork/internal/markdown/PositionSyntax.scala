package vork.internal.markdown

import scala.meta.Input
import scala.meta.Position
import vork.document.RangePosition
import scala.reflect.api.{Position => ReflectPosition}

object PositionSyntax {
  implicit class XtensionInputVork(input: Input) {
    def toOffset(line: Int, column: Int): Position = {
      Position.Range(input, line, column, line, column)
    }
  }
  implicit class XtensionRangePositionVork(pos: RangePosition) {
    def toOriginal(edit: TokenEditDistance): Position = {
      val Right(x) = edit.toOriginal(pos.startLine, pos.startColumn)
      x.toUnslicedPosition
    }
  }
  implicit class XtensionPositionVork(pos: Position) {
    def toUnslicedPosition: Position = pos.input match {
      case Input.Slice(underlying, a, _) =>
        Position.Range(underlying, a + pos.start, a + pos.end)
      case _ =>
        pos
    }
    def contains(offset: Int): Boolean = {
      if (pos.start == pos.end) pos.end == offset
      else {
        pos.start <= offset &&
        pos.end > offset
      }
    }
  }
}

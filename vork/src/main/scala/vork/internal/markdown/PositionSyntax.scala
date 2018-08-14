package vork.internal.markdown

import scala.meta.Input
import scala.meta.Position

object PositionSyntax {
  implicit class XtensionInputVork(input: Input) {
    def toOffset(line: Int, column: Int): Position = {
      Position.Range(input, line, column, line, column)
    }
  }
  implicit class XtensionPositionVork(pos: Position) {
    def contains(offset: Int): Boolean = {
      if (pos.start == pos.end) pos.end == offset
      else {
        pos.start <= offset &&
        pos.end > offset
      }
    }
  }
}

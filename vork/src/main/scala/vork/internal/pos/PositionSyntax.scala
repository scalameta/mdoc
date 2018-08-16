package vork.internal.pos

import java.nio.file.Paths
import scala.meta.Input
import scala.meta.Position
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import scalafix.internal.util.PositionSyntax._
import vork.document.RangePosition
import vork.internal.cli.Settings

object PositionSyntax {
  implicit class XtensionInputVork(input: Input) {
    def filename: String = input match {
      case s: Input.Slice => s.input.filename
      case _ => input.syntax
    }
    def relativeFilename(sourceroot: AbsolutePath): RelativePath = input match {
      case s: Input.Slice =>
        s.input.relativeFilename(sourceroot)
      case _ =>
        AbsolutePath(input.syntax).toRelative(sourceroot)
    }
    def toFilename(settings: Settings): String =
      if (settings.reportRelativePaths) Paths.get(input.filename).getFileName.toString
      else filename
    def toOffset(line: Int, column: Int): Position = {
      Position.Range(input, line, column, line, column)
    }
  }
  implicit class XtensionRangePositionVork(pos: RangePosition) {
    def formatMessage(edit: TokenEditDistance, message: String): String = {
      val mpos = pos.toMeta(edit)
      new StringBuilder()
        .append(message)
        .append("\n")
        .append(mpos.lineContent)
        .append("\n")
        .append(mpos.lineCaret)
        .append("\n")
        .toString()
    }
    def toMeta(edit: TokenEditDistance): Position = {
      Position
        .Range(
          edit.originalInput,
          pos.startLine,
          pos.startColumn,
          pos.endLine,
          pos.endColumn
        )
        .toUnslicedPosition
    }
    def toOriginal(edit: TokenEditDistance): Position = {
      val Right(x) = edit.toOriginal(pos.startLine, pos.startColumn)
      x.toUnslicedPosition
    }
  }
  implicit class XtensionPositionVork(pos: Position) {
    def toUnslicedPosition: Position = pos.input match {
      case Input.Slice(underlying, a, _) =>
        Position.Range(underlying, a + pos.start, a + pos.end).toUnslicedPosition
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

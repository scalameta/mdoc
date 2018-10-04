package mdoc.internal.pos

import java.nio.file.Paths
import scala.meta.Input
import scala.meta.Position
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import scalafix.internal.util.PositionSyntax._
import mdoc.document.RangePosition
import mdoc.internal.cli.Settings
import mdoc.internal.markdown.EvaluatedSection
import scala.util.control.NonFatal

object PositionSyntax {
  implicit class XtensionInputMdoc(input: Input) {
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
  implicit class XtensionRangePositionMdoc(pos: RangePosition) {
    def formatMessage(section: EvaluatedSection, message: String): String = {
      pos.toMeta(section) match {
        case Position.None =>
          message
        case mpos =>
          new StringBuilder()
            .append(message)
            .append("\n")
            .append(mpos.lineContent)
            .append("\n")
            .append(mpos.lineCaret)
            .append("\n")
            .toString()
      }
    }
    def toMeta(section: EvaluatedSection): Position = {
      // FIXME: https://github.com/olafurpg/mdoc/issues/95#issuecomment-426993507
      try toMetaUnsafe(section)
      catch {
        case NonFatal(e) =>
          Position.None
      }
    }
    def toMetaUnsafe(section: EvaluatedSection): Position = {
      val mpos = Position.Range(
        section.input,
        pos.startLine,
        pos.startColumn,
        pos.endLine,
        pos.endColumn
      )
      mpos.toUnslicedPosition
    }
    def toOriginal(edit: TokenEditDistance): Position = {
      val Right(x) = edit.toOriginal(pos.startLine, pos.startColumn)
      x.toUnslicedPosition
    }
  }
  implicit class XtensionPositionMdoc(pos: Position) {
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

package mdoc.internal.pos

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import scala.meta.Input
import scala.meta.Position
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import mdoc.document.RangePosition
import mdoc.internal.cli.CliEnrichments
import mdoc.internal.cli.Settings
import mdoc.internal.markdown.EvaluatedSection
import scala.meta.internal.io.PathIO
import scala.util.control.NonFatal
import coursierapi.Dependency
import scala.meta.internal.io.FileIO
import scala.meta.internal.inputs._

object PositionSyntax extends CliEnrichments with CollectionEnrichments {
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
  implicit class XtensionPositionMdoc2(pos: Position) {
    def toMdoc: RangePosition =
      new RangePosition(
        pos.startLine,
        pos.startColumn,
        pos.endLine,
        pos.endColumn
      )
  }

  def formatMessage(
      pos: Position,
      severity: String,
      message: String,
      includePath: Boolean = true
  ): String =
    pos match {
      case Position.None =>
        if (severity.isEmpty) message
        else s"$severity: $message"
      case _ =>
        new java.lang.StringBuilder()
          .append(if (includePath) pos.lineInput else "")
          .append(if (!includePath || severity.isEmpty) "" else " ")
          .append(severity)
          .append(
            if (message.isEmpty) ""
            else if (severity.isEmpty) " "
            else if (message.startsWith("\n")) ":"
            else ": "
          )
          .append(message)
          .append("\n")
          .append(pos.lineContent)
          .append("\n")
          .append(lineCaret(pos))
          .toString
    }

  private def lineCaret(pos: Position): String =
    pos match {
      case Position.None =>
        ""
      case _ =>
        val caret =
          if (pos.start == pos.end) "^"
          else if (pos.startLine == pos.endLine) "^" * (pos.end - pos.start)
          else "^"
        (" " * pos.startColumn) + caret
    }

  implicit class XtensionPositionsScalafix(private val pos: Position) extends AnyVal {

    def contains(offset: Int): Boolean = {
      if (pos.start == pos.end) pos.end == offset
      else {
        pos.start <= offset &&
        pos.end > offset
      }
    }
    def contains(other: Position): Boolean = {
      pos.start <= other.start &&
      pos.end >= other.end
    }

    def formatMessage(severity: String, message: String): String =
      PositionSyntax.formatMessage(pos, severity, message)

    /** Returns a formatted string of this position including filename/line/caret. */
    def lineInput: String =
      s"${pos.input.syntax}:${pos.startLine + 1}:${pos.startColumn + 1}:"

    def rangeNumber: String =
      s"${pos.startLine + 1}:${pos.startColumn + 1}..${pos.endLine + 1}:${pos.endColumn + 1}"

  }

}

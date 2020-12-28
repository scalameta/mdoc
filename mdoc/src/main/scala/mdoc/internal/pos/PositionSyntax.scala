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
import mdoc.internal.cli.Settings
import mdoc.internal.markdown.EvaluatedSection
import scala.meta.internal.io.PathIO
import scala.util.control.NonFatal
import coursierapi.Dependency
import scala.meta.internal.io.FileIO
import scala.meta.internal.inputs._

object PositionSyntax {
  implicit class XtensionInputMdoc(input: Input) {
    def filename: String =
      input match {
        case s: Input.Slice => s.input.filename
        case _ => input.syntax
      }
    def relativeFilename(sourceroot: AbsolutePath): RelativePath =
      input match {
        case s: Input.Slice =>
          s.input.relativeFilename(sourceroot)
        case _ =>
          AbsolutePath(input.syntax).toRelative(sourceroot)
      }
    def toFilename(settings: Settings): String =
      if (settings.reportRelativePaths) Paths.get(input.filename).getFileName.toString
      else filename
    def toPosition: Position.Range = {
      Position.Range(input, 0, input.chars.length)
    }
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
    def addStart(offset: Int): Position =
      pos match {
        case Position.Range(i, start, end) =>
          Position.Range(i, start + offset, end)
        case _ =>
          pos
      }
    def toUnslicedPosition: Position =
      pos.input match {
        case Input.Slice(underlying, a, _) =>
          Position.Range(underlying, a + pos.start, a + pos.end).toUnslicedPosition
        case _ =>
          pos
      }
    def toMdoc: RangePosition =
      new RangePosition(
        pos.startLine,
        pos.startColumn,
        pos.endLine,
        pos.endColumn
      )
    def contains(offset: Int): Boolean = {
      if (pos.start == pos.end) pos.end == offset
      else {
        pos.start <= offset &&
        pos.end > offset
      }
    }
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

  implicit class XtensionThrowable(e: Throwable) {
    def message: String = {
      if (e.getMessage != null) e.getMessage
      else if (e.getCause != null) e.getCause.message
      else "null"
    }
  }
  implicit class XtensionAbsolutePathLink(path: AbsolutePath) {
    def filename: String = path.toNIO.getFileName.toString
    def extension: String = PathIO.extension(path.toNIO)
    def readText: String = FileIO.slurp(path, StandardCharsets.UTF_8)
    def copyTo(out: AbsolutePath): Unit = {
      Files.createDirectories(path.toNIO.getParent)
      Files.copy(path.toNIO, out.toNIO, StandardCopyOption.REPLACE_EXISTING)
    }
    def write(text: String): Unit = {
      Files.createDirectories(path.toNIO.getParent)
      Files.write(
        path.toNIO,
        text.getBytes(StandardCharsets.UTF_8)
      )
    }
    def toRelativeLinkFrom(other: AbsolutePath, prefix: String): String = {
      prefix + path.toRelative(other.parent).toURI(false).toString
    }
    def parent: AbsolutePath = AbsolutePath(path.toNIO.getParent)
  }
}

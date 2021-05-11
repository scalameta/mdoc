package mdoc.internal.cli

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import scala.meta.Input
import scala.meta.Position
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import scala.meta.internal.io.PathIO
import scala.util.control.NonFatal
import coursierapi.Dependency
import scala.meta.internal.io.FileIO
import scala.meta.internal.inputs._
import java.io.PrintStream

trait CliEnrichments {
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

  implicit class XtensionPrintStream(sb: PrintStream) {

    def appendMultiline(string: String): Unit = {
      appendMultiline(string, string.length)
    }

    def appendMultiline(string: String, N: Int): Unit = {
      var i = 0
      while (i < N) {
        string.charAt(i) match {
          case '\n' =>
            sb.append("\n// ")
          case ch =>
            sb.append(ch)
        }
        i += 1
      }
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
object CliEnrichments extends CliEnrichments

package mdoc.internal.io

import scala.meta._
import scala.collection.mutable
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import mdoc.interfaces.DiagnosticSeverity
import mdoc.document.RangePosition
import mdoc.internal.pos.PositionSyntax._

class StoreReporter() extends ConsoleReporter(System.out) {
  val diagnostics: mutable.LinkedHashSet[Diagnostic] =
    mutable.LinkedHashSet.empty[Diagnostic]

  override def reset(): Unit = diagnostics.clear()

  override def warningCount: Int =
    diagnostics.count(_.severity == DiagnosticSeverity.Warning)
  override def errorCount: Int =
    diagnostics.count(_.severity == DiagnosticSeverity.Error)
  override def hasErrors: Boolean = errorCount > 0
  override def hasWarnings: Boolean = warningCount > 0

  override def warning(pos: Position, msg: String): Unit = {
    diagnostics += new Diagnostic(
      pos.toMdoc,
      msg,
      DiagnosticSeverity.Warning
    )
    super.warning(pos, msg)
  }
  override def error(pos: Position, throwable: Throwable): Unit = {
    val out = new ByteArrayOutputStream()
    throwable.printStackTrace(new PrintStream(out))
    diagnostics += new Diagnostic(
      pos.toMdoc,
      out.toString(),
      DiagnosticSeverity.Error
    )
  }
  override def error(pos: Position, msg: String): Unit = {
    diagnostics += new Diagnostic(
      pos.toMdoc,
      msg,
      DiagnosticSeverity.Error
    )
    super.error(pos, msg)
  }
}

package tests.markdown

import mdoc.internal.io.ConsoleReporter
import mdoc.Reporter
import java.io.PrintStream
import mdoc.internal.io.ConsoleColors
import scala.meta.Position

class TestConsoleReporter(ps: PrintStream, colors: ConsoleColors = ConsoleColors())
    extends ConsoleReporter(ps, colors) {
  private val myWarnings = List.newBuilder[String]

  override def warning(msg: String): Unit = {
    myWarnings += msg
    super.warning(msg)
  }

  override def warning(pos: Position, msg: String): Unit = {
    myWarnings += msg
    super.warning(pos, msg)
  }

  final def warnings: List[String] = myWarnings.result()
}

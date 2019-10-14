package mdoc.internal.markdown

import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.AbstractReporter
import scala.reflect.internal.util.Position

trait VersionSpecificFilteringReporter extends AbstractReporter { self: FilterStoreReporter =>
  override def display(pos: Position, msg: String, severity: Severity): Unit =
    add(pos, msg, severity)
  override def displayPrompt(): Unit = ()
}

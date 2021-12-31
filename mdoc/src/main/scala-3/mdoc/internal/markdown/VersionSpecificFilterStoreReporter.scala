package mdoc.internal.markdown

import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.FilteringReporter
import scala.reflect.internal.util.Position

trait VersionSpecificFilteringReporter extends FilteringReporter { self: FilterStoreReporter =>
  override def doReport(pos: Position, msg: String, severity: Severity): Unit =
    add(pos, msg, severity)
}

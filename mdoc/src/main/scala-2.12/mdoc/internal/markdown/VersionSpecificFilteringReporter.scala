package mdoc.internal.markdown

import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.Reporter
import scala.reflect.internal.util.Position
import scala.reflect.internal.util.NoPosition

trait VersionSpecificFilteringReporter extends Reporter { self: FilterStoreReporter =>
  override def info0(pos: Position, msg: String, severity: Severity, force: Boolean): Unit = {
    if (!infos.exists(info => pos != NoPosition && info.pos.point == pos.point))
      add(pos, msg, severity)
  }

  override def hasErrors: Boolean = infos.exists(_.severity == ERROR)

  override def hasWarnings: Boolean = infos.exists(_.severity == WARNING)

}

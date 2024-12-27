package mdoc.internal.markdown

import scala.collection.mutable
import scala.tools.nsc.Settings
import scala.reflect.internal.util.Position

/** Same as nsc StoreReporter except it extends AbstractReporter.
  *
  * The AbstractReporter does filtering based on position to avoid duplicate diagnostics.
  */
class FilterStoreReporter(val settings: Settings) extends VersionSpecificFilteringReporter {
  case class Info(pos: Position, msg: String, severity: Severity) {
    override def toString() = s"pos: $pos $msg $severity"
  }
  val infos = new mutable.LinkedHashSet[Info]
  protected def add(pos: Position, msg: String, severity: Severity): Unit = {
    infos += Info(pos, msg, severity)
  }
  override def reset(): Unit = {
    super.reset()
    infos.clear()
  }
}

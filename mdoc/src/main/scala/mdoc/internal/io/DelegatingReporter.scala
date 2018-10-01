package mdoc.internal.io
import mdoc.Reporter
import scala.meta.inputs.Position

class DelegatingReporter(underlying: List[Reporter]) extends Reporter {
  override def error(throwable: Throwable): Unit =
    underlying.foreach(_.error(throwable))
  override def error(pos: Position, throwable: Throwable): Unit =
    underlying.foreach(_.error(pos, throwable))
  override def error(pos: Position, msg: String): Unit =
    underlying.foreach(_.error(pos, msg))
  override def error(msg: String): Unit =
    underlying.foreach(_.error(msg))
  override def warning(pos: Position, msg: String): Unit =
    underlying.foreach(_.warning(pos, msg))
  override def warning(msg: String): Unit =
    underlying.foreach(_.warning(msg))
  override def info(pos: Position, msg: String): Unit =
    underlying.foreach(_.info(pos, msg))
  override def info(msg: String): Unit =
    underlying.foreach(_.info(msg))
  override def print(msg: String): Unit =
    underlying.foreach(_.print(msg))
  override def println(msg: String): Unit =
    underlying.foreach(_.println(msg))
  override private[mdoc] def hasWarnings: Boolean =
    underlying.exists(_.hasWarnings)
  override private[mdoc] def hasErrors: Boolean =
    underlying.exists(_.hasErrors)
  override private[mdoc] def reset(): Unit =
    underlying.foreach(_.reset())
}

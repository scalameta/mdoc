package mdoc.internal.markdown

import com.virtuslab.{using_directives => using}
import mdoc.Reporter

import scala.meta.inputs.Input

class UsingReporter(input: Input, reporter: Reporter) extends using.reporter.Reporter {

  import MagicImports._
  override def error(msg: String): Unit = {
    reporter.error(msg)
  }

  override def warning(msg: String): Unit = {

    reporter.warning(msg)
  }

  override def error(position: using.custom.utils.Position, msg: String): Unit = {
    reporter.error(toMetaPosition(input, position), msg)
  }

  override def warning(position: using.custom.utils.Position, msg: String): Unit = {
    reporter.warning(toMetaPosition(input, position), msg)
  }

  override def hasErrors(): Boolean = reporter.hasErrors

  override def hasWarnings(): Boolean = reporter.hasWarnings

  override def reset(): Unit = reporter.reset()

}

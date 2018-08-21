package mdoc.internal.io

import fansi.Color._
import java.io.PrintStream
import scala.meta.Position
import scalafix.internal.util.PositionSyntax._
import mdoc.Reporter
import mdoc.internal.pos.PositionSyntax._

class ConsoleReporter(ps: PrintStream) extends Reporter {

  private val myInfo = Blue("info")
  private val myWarning = Yellow("warning")
  private val myError = Red("error")

  private var myWarnings = 0
  private var myErrors = 0
  def hasWarnings: Boolean = myWarnings > 0
  def hasErrors: Boolean = myErrors > 0
  def reset(): Unit = {
    myWarnings = 0
    myErrors = 0
  }

  def error(throwable: Throwable): Unit = {
    error(Position.None, throwable)
  }

  def error(pos: Position, throwable: Throwable): Unit = {
    error(pos, throwable.getMessage)
    throwable.printStackTrace(ps)
  }
  def error(pos: Position, msg: String): Unit = {
    error(pos.toUnslicedPosition.formatMessage("error", msg))
  }
  def error(msg: String): Unit = {
    myErrors += 1
    ps.println(myError ++ s": $msg")
  }
  def info(pos: Position, msg: String): Unit = {
    info(pos.toUnslicedPosition.formatMessage("info", msg))
  }
  def info(msg: String): Unit = {
    ps.println(myInfo ++ s": $msg")
  }
  def warning(pos: Position, msg: String): Unit = {
    warning(pos.toUnslicedPosition.formatMessage("warning", msg))
  }
  def warning(msg: String): Unit = {
    myWarnings += 1
    ps.println(myWarning ++ s": $msg")
  }

  override def print(msg: String): Unit = {
    ps.print(msg)
  }
  override def println(msg: String): Unit = {
    ps.println(msg)
  }
}

object ConsoleReporter {
  def default: Reporter = new ConsoleReporter(System.out)
}

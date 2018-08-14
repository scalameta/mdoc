package vork.internal.io

import fansi.Color._
import java.io.PrintStream
import scala.meta.Position
import scalafix.internal.util.PositionSyntax._

class Logger(ps: PrintStream) {

  private val myInfo = Blue("info")
  private val myWarning = Yellow("warning")
  private val myError = Red("error")

  private var myErrors = 0
  def hasErrors: Boolean = myErrors > 0
  def reset(): Unit = myErrors = 0

  def error(pos: Position, throwable: Throwable): Unit = {
    error(pos, throwable.getMessage)
    throwable.printStackTrace(ps)
  }
  def error(pos: Position, msg: String): Unit = {
    error(pos.formatMessage("error", msg))
  }
  def error(msg: String): Unit = {
    myErrors += 1
    ps.println(myError ++ s": $msg")
  }
  def info(pos: Position , msg: String): Unit = {
    info(pos.formatMessage("info", msg))
  }
  def info(msg: String): Unit = {
    ps.println(myInfo ++ s": $msg")
  }
  def warning(pos: Position , msg: String): Unit = {
    warning(pos.formatMessage("warning", msg))
  }
  def warning(msg: String): Unit = {
    ps.println(myWarning ++ s": $msg")
  }
}

object Logger {
  def default: Logger = new Logger(System.out)
}

package vork

import java.io.OutputStream
import java.io.PrintStream
import fansi.Color._
import scala.meta.Position
import scala.meta.internal.inputs._

class Logger(out: OutputStream) {

  private val ps = new PrintStream(out)

  private val myInfo = Blue("info")
  private val myWarning = Yellow("warning")
  private val myError = Red("error")

  private var myErrors = 0
  def hasErrors: Boolean = myErrors > 0
  def reset(): Unit = myErrors = 0

  def error(pos: Position, msg: String): Unit = {
    error(pos.formatMessage("error", msg))
  }
  def error(msg: String): Unit = {
    myErrors += 1
    ps.println(myError ++ s": $msg")
  }
  def info(msg: String): Unit = {
    ps.println(myInfo ++ s": $msg")
  }
  def warning(msg: String): Unit = {
    ps.println(myWarning ++ s": $msg")
  }
}

object Logger {
  def default: Logger = new Logger(System.out)
}

package vork

import java.io.OutputStream
import java.io.PrintStream
import fansi.Color._
import scala.meta.Position

class Logger(out: OutputStream) {

  private val ps = new PrintStream(out)

  private val myInfo = Blue("info")
  private val myWarning = Yellow("warning")
  private val myError = Red("error")

  private var myErrors = 0
  def errorCount: Int = myErrors

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

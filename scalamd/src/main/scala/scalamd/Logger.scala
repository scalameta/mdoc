package scalamd

import java.io.OutputStream
import java.io.PrintStream
import fansi.Color._

class Logger(out: OutputStream) {
  private val ps = new PrintStream(out)
  private val myInfo = Blue("info")
  private val myWarning = Yellow("warning")
  private val myError = Red("error")
  def error(msg: String): Unit = ps.println(myError ++ s": $msg")
  def info(msg: String): Unit = ps.println(myInfo ++ s": $msg")
  def warning(msg: String): Unit = ps.println(myWarning ++ s": $msg")
}

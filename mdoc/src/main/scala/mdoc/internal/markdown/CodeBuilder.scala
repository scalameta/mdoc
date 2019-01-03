package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

class CodeBuilder() {
  private val out = new ByteArrayOutputStream()
  private val ps = new PrintStream(out)
  def printIf(cond: Boolean, s: => String): CodeBuilder = {
    if (cond) print(s)
    else this
  }
  def print(s: String): CodeBuilder = {
    ps.print(s)
    this
  }
  def printlnIf(cond: Boolean, s: => String): CodeBuilder = {
    if (cond) println(s)
    else this
  }
  def lines(xs: Iterable[String]): CodeBuilder = {
    xs.iterator.filterNot(_.isEmpty).foreach(println)
    this
  }
  def foreach[T](xs: Iterable[T])(fn: T => Unit): CodeBuilder = {
    xs.foreach(fn)
    this
  }
  def println(s: String): CodeBuilder = {
    if (s.nonEmpty) {
      ps.println(s)
    }
    this
  }

  override def toString: String = {
    out.toString(StandardCharsets.UTF_8.name)
  }
}

package mdoc.internal.markdown

import java.io.PrintStream

class CodePrinter(ps: PrintStream, baseIndent: Int = 0, baseNest: Int = 0) {
  private def indent = "  " * (baseIndent + nestCount)

  private var nestCount = baseNest

  def append(s: String) = { ps.append(s); this }

  def println(s: String) = {
    ps.print(indent + s + "\n"); this
  }

  def definition(header: String)(cb: CodePrinter => Unit): CodePrinter = {
    val newCB = new CodePrinter(ps, baseIndent, nestCount)

    this.println(header + " {")
    cb(newCB)
    this.println("}")

    this
  }

  def appendLines(body: String, nest: Boolean = false) = {
    if (nest) nestCount += 1
    body.linesIterator.toArray.foreach(this.println)
    if (nest) nestCount -= 1
    this
  }

  def line(f: StringBuilder => Unit) = {
    val sb = new StringBuilder
    f(sb)

    this.println(sb.result())
    this
  }

  def nest(): Unit = {
    this.println("_root_.scala.Predef.locally {")
    nestCount += 1
  }

  def unnest(): Unit = {
    this.println("};" * nestCount)
    nestCount = baseNest
  }
}

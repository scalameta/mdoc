package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream

final class FailInstrumenter(sections: List[SectionInput], i: Int) {
  private val out = new ByteArrayOutputStream()
  private val sb = new PrintStream(out)
  private val gensym = new Gensym()
  def instrument(): String = {
    printAsScript()
    out.toString
  }
  private def printAsScript(): Unit = {
    sb.println("package repl")
    sb.println("object Session {")
    sb.println("  object App {")
    sections.zipWithIndex.foreach {
      case (section, j) =>
        if (j > i) ()
        else {
          if (section.mod.isReset) {
            sb.print(Instrumenter.reset(section.mod, gensym.fresh("App")))
          }
          if (j == i || !section.mod.isFail) {
            sb.println(section.input.text)
          }
        }
    }
    sb.println("\n  }\n}")
  }
}

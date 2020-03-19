package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream

final class FailInstrumenter(sections: List[SectionInput], i: Int) {
  private val out = new ByteArrayOutputStream()
  private val sb = new PrintStream(out)
  private val gensym = new Gensym()
  private val nest = new Nesting(sb)
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
            nest.unnest()
            sb.print(Instrumenter.reset(section.mod, gensym.fresh("App")))
          } else if (section.mod.isNest) {
            nest.nest()
          }
          if (j == i || !section.mod.isFailOrWarn) {
            sb.println(section.input.text)
          }
        }
    }
    sb.println("\n  }\n}")
    nest.unnest()
  }
}

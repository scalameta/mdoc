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
    sb.println("class Session {")
    sb.println("  def app() = {")
    sections.zipWithIndex.foreach {
      case (section, j) =>
        if (j > i) ()
        else {
          if (section.mod == Modifier.Reset) {
            val nextApp = gensym.fresh("app", "()")
            sb.print(s"this.$nextApp\n}\ndef $nextApp: Unit = {\n")
          }
          if (j == i || section.mod != Modifier.Fail) {
            sb.println(section.input.text)
          }
        }
    }
    sb.println("\n  }\n}")
  }
}

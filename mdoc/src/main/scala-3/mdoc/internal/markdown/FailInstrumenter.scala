package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.meta._

final class FailInstrumenter(sections: List[SectionInput], i: Int) {
  private val out = new ByteArrayOutputStream()
  private val gensym = new Gensym()
  def instrument(): String = {
    printAsScript()
    out.toString
  }

  val snip = new CodePrinter(new PrintStream(out))
  private def printAsScript(): Unit = {
    snip.println("package repl")
    snip.definition("object MdocSession") {
      _.definition("object MdocApp") { sb =>
        sections.zipWithIndex.foreach { case (section, j) =>
          if (j > i) ()
          else {
            if (section.mod.isReset) {
              sb.unnest()
              sb.append(Instrumenter.reset(section.mod, gensym.fresh("MdocApp")))
            } else if (section.mod.isNest) {
              sb.nest()
            }
            if (j == i || !section.mod.isFailOrWarn) {
              section.source.stats.foreach { stat =>
                stat match {
                  case i: Import =>
                    i.importers.foreach {
                      case Importer(
                            Term.Name(name),
                            List(Importee.Name(_: Name.Indeterminate))
                          ) if Instrumenter.magicImports(name) =>
                      case importer =>
                        sb.line {
                          _.append("import ")
                            .append(importer.pos.text)
                            .append(";")
                        }
                    }
                  case _ =>
                    sb.appendLines(stat.pos.text)
                }
              }
            }
          }
        }
        sb.unnest()
      }
    }
  }
}

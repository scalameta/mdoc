package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.meta._

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
    sb.println("object MdocSession {")
    sb.println("  object App {")

    sections.zipWithIndex.foreach { case (section, j) =>
      if (j > i) ()
      else {
        section.mod match {
          case fenceModifier: Modifier =>
            if (fenceModifier.isReset) {
              nest.unnest()
              sb.print(Instrumenter.reset(fenceModifier, gensym.fresh("App")))
            } else if (fenceModifier.isNest) {
              nest.nest()
            }
            if (j == i || !fenceModifier.isFailOrWarn) {
              println("Should proceed for fence: " + section.source)
              section.source.stats.foreach { stat =>
                stat match {
                  case i: Import =>
                    i.importers.foreach {
                      case Importer(
                      Term.Name(name),
                      List(Importee.Name(_: Name.Indeterminate))
                      ) if Instrumenter.magicImports(name) =>
                      case importer =>
                        sb.print("import ")
                        sb.print(importer.pos.text)
                        sb.print(";")
                    }
                  case _ =>
                    sb.println(stat.pos.text)
                }
              }
            }
          case modifierInline: ModifierInline =>
            println("FailInstrumenter.modifierInline branch")
            nest.nest()
            println("j: " + j)
            println("i: " + i)
            sb.println(section.input)
            if (j == i || !modifierInline.isFailOrWarn) {
              println("Should proceed: " + section.source)
              section.source.stats.foreach { stat =>
                println("stat: " + stat)
                stat match {
                  case i: Import =>
                    println("Uh? Import?")
                    i.importers.foreach {
                      case Importer(
                      Term.Name(name),
                      List(Importee.Name(_: Name.Indeterminate))
                      ) if Instrumenter.magicImports(name) =>
                      case importer =>
                        sb.print("import ")
                        sb.print(importer.pos.text)
                        sb.print(";")
                    }
                  case _ =>
                    println("stat.pos.text: " + stat.pos.text)
                    sb.println(stat.pos.text)
                }
              }
            }
        }

      }
    }
    sb.println("\n  }\n}")
    nest.unnest()
  }
}

package vork.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.meta.Name
import vork.internal.cli.Semantics
import vork.internal.markdown.MarkdownCompiler.Binders
import vork.internal.markdown.MarkdownCompiler.position
import vork.internal.markdown.MarkdownCompiler.SectionInput

object Instrumenter {
  def instrument(semantics: Semantics, sections: List[SectionInput]): String = {
    val body = semantics match {
      case Semantics.REPL => ???
      case Semantics.Script => script(sections)
    }
    wrapBody(body)
  }

  def script(sections: List[SectionInput]): String = {
    val out = new ByteArrayOutputStream()
    val sb = new PrintStream(out)
    var i = 0
    def freshBinder(): String = {
      val name = s"res$i"
      i += 1
      name
    }
    sections.foreach { section =>
      sb.println()
      sb.println("$doc.sect();")
      section.source.stats.foreach { stat =>
        sb.println("$doc.stat();")
        val binders = stat match {
          case Binders(names) =>
            names.map(name => name -> name.pos)
          case _ =>
            val fresh = freshBinder()
            sb.print(s"val $fresh = ")
            List(Name(fresh) -> stat.pos)
        }
        sb.print(stat.syntax)
        binders.foreach {
          case (name, pos) =>
            sb.print(s"; $$doc.binder($name, ${position(pos)})")
        }
        sb.println()
      }
    }
    out.toString()
  }

  def wrapBody(body: String): String = {
    val wrapped = new StringBuilder()
      .append("package repl\n")
      .append("class Session extends _root_.vork.internal.document.DocumentBuilder {\n")
      .append("  def app(): Unit = {\n")
      .append(body)
      .append("  }\n")
      .append("}\n")
      .toString()
    wrapped
  }
}

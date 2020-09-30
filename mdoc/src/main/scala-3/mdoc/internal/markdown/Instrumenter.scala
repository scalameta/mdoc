package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path

import dotty.tools.dotc.interfaces.SourcePosition
import dotty.tools.dotc.ast.untpd._
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.ast.Trees
import dotty.tools.dotc.core.Flags

import scala.collection.mutable
import mdoc.Reporter
import mdoc.internal.cli.InputFile
import mdoc.internal.cli.Settings
import scala.meta.Mod.Lazy
import scala.meta.Name

import Instrumenter.position

/* This Class can be removed once Scalameta parser is ready for Scala 3 code
 * we should only make sure that the other Instrumenter generates code with proper indentation 
 * */
class Instrumenter(
    file: InputFile,
    sections: List[SectionInput],
    settings: Settings,
    reporter: Reporter
) {
  private val innerClassIdent = " " * 4
  def instrument(): Instrumented = {
    printAsScript()
    Instrumented.fromSource(
      out.toString,
      magic.scalacOptions.toList,
      magic.dependencies.toList,
      magic.repositories.toList,
      Nil,
      reporter
    )
  }

  val magic = new MagicImports(settings, reporter, file)
  private val out = new ByteArrayOutputStream()
  private val sb = new PrintStream(out)
  val gensym = new Gensym()
  val nest = new Nesting(sb)

  private def printlnWithIndent(out: String) = {
    sb.println(innerClassIdent + out)
  }

  private def printWithIndent(out: String) = {
    sb.print(innerClassIdent + out)
  }

  private def printAsScript(): Unit = {
    sections.zipWithIndex.foreach {
      case (section, i) =>
        import section.ctx
        printlnWithIndent("")
        printlnWithIndent("$doc.startSection();")
          section.stats.foreach { stat =>
            printlnWithIndent(s"$$doc.startStatement(${position(stat.sourcePos)});")
            printStatement(stat, section.mod, sb, section)
            printlnWithIndent("")
            printlnWithIndent("$doc.endStatement();")
          }
        printlnWithIndent("$doc.endSection();")
    }
    nest.unnest()
  }

  private def printBinder(name: String, pos: SourcePosition): Unit = {
    printlnWithIndent("")
    printlnWithIndent(s"""$$doc.binder($name, ${position(pos)})""")
  }
  private def printStatement(
      stat: Tree,
      m: Modifier,
      sb: PrintStream,
      section: SectionInput
  )(using ctx: Context): Unit = {
    val binders = stat match {
      case Instrumenter.Binders(names) =>
        names
      case _ =>
        val fresh = gensym.fresh("res")
        printWithIndent(s"val $fresh = ")
        List(fresh -> stat.sourcePos)
    }
    stat match {
      case magic.NonPrintable() =>
      case _ =>
        printWithIndent(section.show(stat, innerClassIdent.size))
    }
    binders.foreach {
      case (name, pos) =>
        printBinder(
          name,
          pos
        )
    }
  }
}
object Instrumenter {
  val magicImports = Set(
    "$file",
    "$scalac",
    "$repo",
    "$dep",
    "$ivy"
  )
  def instrument(
      file: InputFile,
      sections: List[SectionInput],
      settings: Settings,
      reporter: Reporter
  ): Instrumented = {
    val instrumented = new Instrumenter(file, sections, settings, reporter).instrument()
    instrumented.copy(source = wrapBody(instrumented.source))
  }

  def position(pos: SourcePosition): String = {
    val start = SectionInput.startLine
    val ident = SectionInput.startIdent
    s"${pos.startLine - start}, ${pos.startColumn - ident}, ${pos.endLine - start}, ${pos.endColumn - ident}"
  }

  def wrapBody(body: String): String = {
    val wrapped = new StringBuilder()
      .append("package repl\n")
      .append("object MdocSession extends _root_.mdoc.internal.document.DocumentBuilder {\n")
      .append("  def app(): _root_.scala.Unit = {val _ = new App()}\n")
      .append("  class App {\n")
      .append(body)
      .append("  }\n")
      .append("}\n")
      .toString()
    wrapped
  }

  object Binders {
    private def fromPat(trees: List[Tree])(using ctx: Context) = {
      trees.collect {
        case id: Ident if id.name.toString != "_" => // ignore placeholders
          id.name.toString -> id.sourcePos
      }
    }
    def unapply(tree: Tree)(using ctx: Context): Option[List[(String, SourcePosition)]] =
      tree match {
        case df: Trees.ValDef[_] if df.mods.is(Flags.Lazy) => Some(Nil)
        case df: Trees.ValDef[_] =>
          Some(List(df.name.toString -> df.sourcePos))
        case pat: PatDef =>
          val pattern = pat.pats.flatMap {
            case tpl: Tuple =>
              fromPat(tpl.trees)
            case appl: Apply =>
              fromPat(appl.args)
            case _ => Nil
          }
          Some(pattern)
        case _: Trees.DefDef[_] => Some(Nil)
        case _: Import => Some(Nil)
        case _: Trees.DefTree[_] => Some(Nil)
        case _: ExtMethods => Some(Nil)
        case _ => None
      }
  }

}

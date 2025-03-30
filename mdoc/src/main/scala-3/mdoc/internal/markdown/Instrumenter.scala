package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import scala.meta._
import scala.meta.inputs.Position
import Instrumenter.position
import mdoc.internal.markdown.Instrumenter.Binders
import scala.meta.Mod.Lazy
import scala.collection.mutable
import mdoc.Reporter
import mdoc.internal.cli.InputFile
import java.nio.file.Path
import mdoc.internal.cli.Settings

class Instrumenter(
    file: InputFile,
    sections: List[SectionInput],
    settings: Settings,
    reporter: Reporter
) {
  def instrument(): Instrumented = {
    printAsScript()
    Instrumented.fromSource(
      out.toString,
      magic.scalacOptions.toList,
      magic.dependencies.toList,
      magic.repositories.toList,
      magic.files.values.toList,
      reporter
    )
  }
  val magic = new MagicImports(settings, reporter, file)
  sections.foreach { section =>
    magic.findUsingDirectives(section.input)
    magic.visitUsingFile(section.input)
  }
  private val out = new ByteArrayOutputStream()
  val gensym = new Gensym()
  val sb = new CodePrinter(new PrintStream(out))

  private def printAsScript(): Unit = {
    sections.zipWithIndex.foreach { case (section, i) =>
      if (section.mod.isReset) {
        sb.unnest()
        sb.println(Instrumenter.reset(section.mod, gensym.fresh("MdocApp")))
      } else if (section.mod.isNest) {
        sb.nest()
      }
      sb.println("$doc.startSection();")
      if (section.mod.isFailOrWarn) {
        sb.println(s"$$doc.startStatement(${position(section.source.pos)});")
        val out = new FailInstrumenter(sections, i).instrument()
        val literal = Instrumenter.stringLiteral(out)
        val binder = gensym.fresh("res")
        sb.line {
          _.append("val ")
            .append(binder)
            .append(" = _root_.mdoc.internal.document.FailSection(")
            .append(literal)
            .append(", ")
            .append(position(section.source.pos))
            .append(");")
        }
        printBinder(binder, section.source.pos)
        sb.println("$doc.endStatement();")
      } else if (section.mod.isCompileOnly) {
        section.source.stats.foreach { stat =>
          sb.println(s"$$doc.startStatement(${position(stat.pos)});")
          sb.println("$doc.endStatement();")
        }
        sb.definition(s"""object ${gensym.fresh("compile")}""") {
          _.appendLines(section.source.pos.text)
        }
      } else if (section.mod.isCrash) {
        section.source.stats match {
          case head :: _ =>
            sb.println(s"$$doc.startStatement(${position(head.pos)});")

            sb.definition("$doc.crash(" ++ position(head.pos) ++ ")") { cb =>
              section.source.stats.foreach { stat =>
                cb.appendLines(stat.pos.text)
              }
            }

            sb.println("\n") // newline for posterity
            sb.println("$doc.endStatement();")

          case Nil =>
        }
      } else {
        section.source.stats.foreach { stat =>
          sb.println(s"$$doc.startStatement(${position(stat.pos)});")
          printStatement(stat, section.mod, sb)
          sb.println("$doc.endStatement();")
        }
      }
      sb.println("$doc.endSection();")
    }
    sb.unnest()
  }

  private def printBinder(name: String, pos: Position): Unit = {
    sb.println(s"$$doc.binder($name, ${position(pos)});")
  }
  private def printStatement(stat: Tree, m: Modifier, sb: CodePrinter): Unit = {
    if (!m.isCrash) {
      val (fresh, binders) = stat match {
        case Binders(names) =>
          (false, names.map(name => name -> name.pos))
        case _: Term.EndMarker => (false, Nil)
        case _ =>
          val fresh = gensym.fresh("res")
          sb.line { _.append(s"val $fresh = ") }
          (true, List(Name(fresh) -> stat.pos))
      }
      stat match {
        case i: Import =>
          def printImporter(importer: Importer): Unit = {
            sb.line {
              _.append("import ")
                .append(importer.pos.text)
                .append(";")
            }
          }
          i.importers.foreach {
            case importer @ magic.Printable(_) =>
              printImporter(importer)
            case magic.NonPrintable() =>
            case importer =>
              printImporter(importer)
          }
        case _: Term.EndMarker =>
        case _ =>
          sb.appendLines(stat.pos.text, fresh)
      }
      binders.foreach { case (name, pos) =>
        printBinder(name.syntax, pos)
      }
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
  def reset(mod: Modifier, identifier: String): String = {
    val ctor =
      if (mod.isResetClass) s"new $identifier()"
      else identifier
    val keyword =
      if (mod.isResetClass) "class"
      else "object"
    s"$ctor\n}\n$keyword $identifier {\n"
  }
  def instrument(
      file: InputFile,
      sections: List[SectionInput],
      settings: Settings,
      reporter: Reporter
  ): Instrumented = {
    val instrumented = new Instrumenter(file, sections, settings, reporter).instrument()
    instrumented.copy(source = wrapBody(instrumented.source))
  }

  def position(pos: Position): String = {
    s"${pos.startLine}, ${pos.startColumn}, ${pos.endLine}, ${pos.endColumn}"
  }

  def stringLiteral(string: String): String = {
    import scala.meta.internal.prettyprinters._
    DoubleQuotes(string)
  }

  def wrapBody(body: String): String = {
    val wrapped = new ByteArrayOutputStream()

    val ps = new PrintStream(wrapped)
    val cb = new CodePrinter(ps)
    cb.println("package repl")
    cb.definition("object MdocSession extends _root_.mdoc.internal.document.DocumentBuilder") {
      _.println("def app(): _root_.scala.Unit = {val _ = new MdocApp()}")
        .definition("class MdocApp") {
          _.appendLines(body)
        }

    }

    wrapped.toString()
  }
  object Binders {
    def binders(pat: Pat): List[Name] =
      pat.collect { case m: Member => m.name }
    def unapply(tree: Tree): Option[List[Name]] =
      tree match {
        case t: Defn.Val if t.mods.exists(_.isInstanceOf[Lazy]) => Some(Nil)
        case t: Tree.WithPats with Defn => Some(t.pats.flatMap(binders))
        case _: Defn => Some(Nil)
        case _: Import => Some(Nil)
        case _ => None
      }
  }

}

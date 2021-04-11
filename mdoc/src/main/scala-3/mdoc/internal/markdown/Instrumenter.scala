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

class CodePrinter(ps: PrintStream, baseIndent: Int = 0, baseNest: Int = 0) {
  private def indent = "  " * (baseIndent + nestCount)

  private var nestCount = baseNest

  def append(s: String) = {ps.append(s); this }
  
  def println(s: String) = {ps.print(indent + s + "\n"); this}

  def definition(header: String)(cb: CodePrinter => Unit): CodePrinter = {
    val newCB = new CodePrinter(ps, baseIndent + 1, baseNest)

    this.println(header + " {")
    cb(newCB)
    this.println("}")

    this
  }

  def appendLines(body: String) = {
    body.linesIterator.toArray.foreach(this.println)
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
  private val out = new ByteArrayOutputStream()
  val gensym = new Gensym()
  val sb = new CodePrinter(new PrintStream(out))

  private def printAsScript(): Unit = {
    sections.zipWithIndex.foreach { case (section, i) =>
      if (section.mod.isReset) {
        sb.unnest()
        sb.println(Instrumenter.reset(section.mod, gensym.fresh("App")))
      } else if (section.mod.isNest) {
        sb.nest()
      }
      sb.println("$doc.startSection();")
      if (section.mod.isFailOrWarn) {
        sb.println(s"$$doc.startStatement(${position(section.source.pos)});")
        val out = new FailInstrumenter(sections, i).instrument()
        val literal = Instrumenter.stringLiteral(out)
        val binder = gensym.fresh("res")
        sb.line{_.append("val ")
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
          _.println(section.source.pos.text)
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
    if (m.isCrash) {
      sb.definition("$doc.crash(" ++ position(stat.pos) ++ ")") {
        _.appendLines(stat.pos.text)
      }
    } else {
      val binders = stat match {
        case Binders(names) =>
          names.map(name => name -> name.pos)
        case _ =>
          val fresh = gensym.fresh("res")
          sb.line{_.append(s"val $fresh = ")}
          List(Name(fresh) -> stat.pos)
      }
      stat match {
        case i: Import =>
          def printImporter(importer: Importer): Unit = {
              sb.line {_.append("import ")
              .append(importer.syntax)
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
        case _ =>
          sb.appendLines(stat.pos.text)
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
    enquote(string, DoubleQuotes)
  }

  def wrapBody(body: String): String = {
    val wrapped = new ByteArrayOutputStream()

    val ps = new PrintStream(wrapped)
    val cb = new CodePrinter(ps)
    cb.println("package repl")
    cb.definition("object MdocSession extends _root_.mdoc.internal.document.DocumentBuilder") {
      _.println("def app(): _root_.scala.Unit = {val _ = new App()}")
       .definition("class App") {
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
        case Defn.Val(mods, _, _, _) if mods.exists(_.isInstanceOf[Lazy]) => Some(Nil)
        case Defn.Val(_, pats, _, _) => Some(pats.flatMap(binders))
        case Defn.Var(_, pats, _, _) => Some(pats.flatMap(binders))
        case _: Defn => Some(Nil)
        case _: Import => Some(Nil)
        case _ => None
      }
  }

}

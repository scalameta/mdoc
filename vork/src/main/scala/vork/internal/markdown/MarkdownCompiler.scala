package vork.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import scala.meta._
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.io.VirtualDirectory
import scala.tools.nsc.reporters.StoreReporter
import scalafix.v0._
import vork.Reporter
import vork.document.CompileResult
import vork.document.CrashResult
import vork.document.CrashResult.Crashed
import vork.document.Document
import vork.document.Section
import vork.document._
import vork.internal.cli.Semantics
import vork.internal.document.DocumentBuilder
import vork.internal.document.VorkExceptions
import vork.internal.pos.PositionSyntax._
import vork.internal.pos.TokenEditDistance

object MarkdownCompiler {

  def buildDocument(
      compiler: MarkdownCompiler,
      original: List[SectionInput],
      instrumented: String,
      reporter: Reporter,
      filename: String
  ): Document = {
    // Use string builder to avoid accidental stripMargin processing
    val instrumentedInput = InstrumentedInput(filename, instrumented)
    val compileInput = Input.VirtualFile(filename, instrumented)
    val edit = toTokenEdit(original.map(_.source), compileInput)
    compiler.compile(compileInput, reporter, edit) match {
      case Some(loader) =>
        val cls = loader.loadClass("repl.Session")
        val doc = cls.newInstance().asInstanceOf[DocumentBuilder].$doc
        try {
          doc.build(instrumentedInput)
        } catch {
          case e: PositionedException =>
            val input = original(e.section).input
            val pos =
              if (e.pos.isEmpty) {
                Position.Range(input, 0, 0)
              } else {
                val slice = Position.Range(
                  input,
                  e.pos.startLine,
                  e.pos.startColumn,
                  e.pos.endLine,
                  e.pos.endColumn
                )
                slice.toUnslicedPosition
              }
            reporter.error(pos, e.getCause)
            Document.empty(instrumentedInput)
        }
      case None =>
        // An empty document will render as the original markdown
        Document.empty(instrumentedInput)
    }
  }

  def toTokenEdit(original: Seq[Tree], instrumented: Input): TokenEditDistance = {
    val instrumentedTokens = instrumented.tokenize.get
    val originalTokens: Array[Token] = {
      val buf = Array.newBuilder[Token]
      original.foreach { tree =>
        tree.tokens.foreach { token =>
          buf += token
        }
      }
      buf.result()
    }
    TokenEditDistance(originalTokens, instrumentedTokens)
  }

  case class EvaluatedDocument(
      instrumented: Input,
      edit: TokenEditDistance,
      sections: List[EvaluatedSection]
  )
  object EvaluatedDocument {
    def apply(document: Document, trees: List[SectionInput]): EvaluatedDocument = {
      val instrumented =
        Input.VirtualFile(document.instrumented.filename, document.instrumented.text)
      val edit = toTokenEdit(trees.map(_.source), instrumented)
      EvaluatedDocument(
        instrumented,
        edit,
        document.sections.zip(trees).map {
          case (a, b) => EvaluatedSection(a, b.input, b.source, b.mod)
        }
      )
    }
  }
  case class EvaluatedSection(section: Section, input: Input, source: Source, mod: Modifier) {
    def out: String = section.statements.map(_.out).mkString
  }

  def fromClasspath(classpath: String): MarkdownCompiler = {
    val fullClasspath =
      if (classpath.isEmpty) defaultClasspath(_ => true)
      else {
        val base = defaultClasspath(_ => true)
        val runtime = defaultClasspath(path => path.toString.contains("vork-runtime"))
        base ++ runtime
      }
    new MarkdownCompiler(fullClasspath.syntax)
  }

  def default(): MarkdownCompiler = fromClasspath("")

  def render(
      semantics: Semantics,
      sections: List[Input],
      compiler: MarkdownCompiler,
      reporter: Reporter,
      filename: String
  ): EvaluatedDocument = {
    val inputs =
      sections.map(s => SectionInput(s, dialects.Sbt1(s).parse[Source].get, Modifier.Default))
    val instrumented = Instrumenter.instrument(semantics, inputs)
    renderInputs(
      instrumented,
      inputs,
      compiler,
      reporter,
      filename
    )
  }

  case class SectionInput(input: Input, source: Source, mod: Modifier)

  def renderInputs(
      instrumented: String,
      sections: List[SectionInput],
      compiler: MarkdownCompiler,
      reporter: Reporter,
      filename: String
  ): EvaluatedDocument = {
    val doc = buildDocument(compiler, sections, instrumented, reporter, filename)
    val evaluated = EvaluatedDocument(doc, sections)
    evaluated
  }

  def renderCrashSection(
      section: EvaluatedSection,
      reporter: Reporter,
      edit: TokenEditDistance
  ): String = {
    require(section.mod.isCrash, section.mod)
    val out = new ByteArrayOutputStream()
    val ps = new PrintStream(out)
    ps.println("```scala")
    ps.println(section.source.syntax)
    val crashes = for {
      statement <- section.section.statements
      binder <- statement.binders
      if binder.value.isInstanceOf[Crashed]
    } yield binder.value.asInstanceOf[Crashed]
    crashes.headOption match {
      case Some(CrashResult.Crashed(e, _)) =>
        VorkExceptions.trimStacktrace(e)
        e.printStackTrace(new PrintStream(out))
      case None =>
        val mpos = section.source.pos
        reporter.error(mpos, "Expected runtime exception but program completed successfully")
    }
    ps.println("```")
    out.toString()
  }
  def renderEvaluatedSection(
      doc: EvaluatedDocument,
      section: EvaluatedSection,
      reporter: Reporter
  ): String = {
    val sb = new StringBuilder
    var first = true
    section.section.statements.zip(section.source.stats).foreach {
      case (statement, tree) =>
        if (first) {
          first = false
        } else {
          sb.append("\n")
        }
        sb.append("@ ")
          .append(tree.syntax)
        if (statement.out.nonEmpty) {
          sb.append("\n").append(statement.out)
        }
        if (sb.charAt(sb.length() - 1) != '\n') {
          sb.append("\n")
        }

        statement.binders.foreach { binder =>
          section.mod match {
            case Modifier.Fail =>
              binder.value match {
                case CompileResult.TypecheckedOK(_, tpe, pos) =>
                  val mpos = Position
                    .Range(
                      section.input,
                      pos.startLine,
                      pos.startColumn,
                      pos.endLine,
                      pos.endColumn
                    )
                    .toUnslicedPosition
                  reporter.error(
                    mpos,
                    s"Expected compile error but statement type-checked successfully"
                  )
                  sb.append(s"// $tpe")
                case CompileResult.ParseError(msg, pos) =>
                  sb.append(pos.formatMessage(doc.edit, msg))
                case CompileResult.TypeError(msg, pos) =>
                  sb.append(pos.formatMessage(doc.edit, msg))
                case _ =>
                  val obtained = pprint.PPrinter.BlackWhite.apply(binder).toString()
                  throw new IllegalArgumentException(
                    s"Expected Macros.CompileResult." +
                      s"Obtained $obtained"
                  )
              }
            case Modifier.Default | Modifier.Passthrough =>
              sb.append(binder.name)
                .append(": ")
                .append(binder.tpe.render)
                .append(" = ")
                .append(pprint.PPrinter.BlackWhite.apply(binder.value))
                .append("\n")
            case Modifier.Crash =>
              throw new IllegalArgumentException(Modifier.Crash.toString)
            case c: Modifier.Str =>
              throw new IllegalArgumentException(c.toString)
          }
        }
    }
    if (sb.nonEmpty && sb.last == '\n') sb.setLength(sb.length - 1)
    sb.toString
  }

  def defaultClasspath(fn: Path => Boolean): Classpath = {
    val paths =
      getClass.getClassLoader
        .asInstanceOf[URLClassLoader]
        .getURLs
        .iterator
        .map(url => AbsolutePath(Paths.get(url.toURI)))
    Classpath(paths.toList)
  }

  def instrumentSections(sections: List[SectionInput]): String = {
    var counter = 0
    val totalStats = sections.map(_.source.stats.length).sum
    val mapped = sections.map { section =>
      val (instrumentedSection, nextCounter) = instrument(section, counter)
      counter = nextCounter
      WIDTH_INDENT +
        "; $doc.section {\n" +
        instrumentedSection
    }
    val join = """
// =======
// Section
// =======
"""
    val end = "\n" + ("}" * (totalStats + sections.length))
    mapped.mkString("", join, end)
  }

  object Binders {
    def binders(pat: Pat): List[Name] =
      pat.collect { case m: Member => m.name }
    def unapply(tree: Tree): Option[List[Name]] = tree match {
      case Defn.Val(_, pats, _, _) => Some(pats.flatMap(binders))
      case Defn.Var(_, pats, _, _) => Some(pats.flatMap(binders))
      case _: Defn => Some(Nil)
      case _: Import => Some(Nil)
      case _ => None
    }
  }

  def literal(string: String): String = {
    import scala.meta.internal.prettyprinters._
    enquote(string, DoubleQuotes)
  }

  private val WIDTH = 100
  private val WIDTH_INDENT = " " * WIDTH
  private val VORK = "/* $vork */"
  def stripVorkSuffix(message: String): String = {
    val idx = message.indexOf(VORK)
    if (idx < 0) message
    else message.substring(0, idx)
  }

  def position(pos: Position): String = {
    s"${pos.startLine}, ${pos.startColumn}, ${pos.endLine}, ${pos.endColumn}"
  }

  def instrument(section: SectionInput, n: Int): (String, Int) = {
    var counter = n
    val source = section.source
    val stats = source.stats
    val ctx = RuleCtx(source)
    def freshBinder(): String = {
      val name = "res" + counter
      counter += 1
      name
    }
    val rule = Rule.syntactic("Vork") { ctx =>
      val patches = stats.map { stat =>
        val (names, freshBinderPatch) = Binders.unapply(stat) match {
          case Some(b) if section.mod != Modifier.Fail =>
            b -> Patch.empty
          case _ =>
            val name = freshBinder()
            List(Term.Name(name)) -> ctx.addLeft(stat, s"${WIDTH_INDENT}val $name = \n")
        }
        val statPositions = position(stat.pos)
        val failPatch = section.mod match {
          case Modifier.Fail =>
            val newCode =
              s"_root_.vork.internal.document.Macros.fail(${literal(stat.syntax)}, $statPositions); "
            ctx.replaceTree(stat, newCode)
          case _ =>
            Patch.empty
        }
        val rightIndent = " " * (WIDTH - stat.pos.endColumn)
        val positionPatch =
          if (section.mod.isDefault) {
            val statPosition = s"$$doc.position($statPositions); \n"
            ctx.addLeft(stat, statPosition)
          } else {
            Patch.empty
          }

        val binders = names
          .map(name => s"$$doc.binder($name, ${position(name.pos)})")
          .mkString(rightIndent + VORK + "; ", "; ", s"; $$doc.statement { ")
        failPatch +
          positionPatch +
          freshBinderPatch +
          ctx.addRight(stat, binders)
      }
      val patch = patches.asPatch
      if (section.mod.isCrash) {
        ctx.addLeft(ctx.tree, s"$$doc.crash(${position(ctx.tree.pos)}) {") +
          ctx.addRight(ctx.tree, s"}") +
          patch
      } else {
        patch
      }
    }
    val out = rule.apply(ctx)
    out -> counter
  }
}

class MarkdownCompiler(
    classpath: String,
    target: AbstractFile = new VirtualDirectory("(memory)", None)
) {
  private val settings = new Settings()
  settings.deprecation.value = true // enable detailed deprecation warnings
  settings.unchecked.value = true // enable detailed unchecked warnings
  settings.outputDirs.setSingleOutput(target)
  settings.classpath.value = classpath
  lazy val sreporter = new StoreReporter
  private val global = new Global(settings, sreporter)
  private val appClasspath: Array[URL] = classpath
    .split(File.pathSeparator)
    .map(path => new File(path).toURI.toURL)
  private val appClassLoader = new URLClassLoader(
    appClasspath,
    this.getClass.getClassLoader
  )
  private def classLoader = new AbstractFileClassLoader(target, appClassLoader)

  private def clearTarget(): Unit = target match {
    case vdir: VirtualDirectory => vdir.clear()
    case _ =>
  }

  def compile(input: Input, vreporter: Reporter, edit: TokenEditDistance): Option[ClassLoader] = {
    clearTarget()
    sreporter.reset()
    val run = new global.Run
    val label = input match {
      case Input.File(path, _) => path.toString()
      case Input.VirtualFile(path, _) => path
      case _ => "(input)"
    }
    run.compileSources(List(new BatchSourceFile(label, new String(input.chars))))
    if (!sreporter.hasErrors) {
      Some(classLoader)
    } else {
      sreporter.infos.foreach {
        case sreporter.Info(pos, msg, severity) =>
          val mpos = edit.toOriginal(pos.point) match {
            case Left(_) =>
              Position.None
            case Right(p) => p.toUnslicedPosition
          }
          severity match {
            case sreporter.ERROR => vreporter.error(mpos, msg)
            case sreporter.INFO => vreporter.info(mpos, msg)
            case sreporter.WARNING => vreporter.warning(mpos, msg)
          }
        case _ =>
      }
      None
    }
  }
}

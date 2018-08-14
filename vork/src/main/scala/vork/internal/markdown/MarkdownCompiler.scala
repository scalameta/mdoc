package vork.internal.markdown

import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.net.URLDecoder
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
import vork.document.CompileResult
import vork.document.Section
import vork.document.{Document, _}
import vork.internal.document.DocumentBuilder
import PositionSyntax._
import vork.Reporter

object MarkdownCompiler {

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
          case (a, b) => EvaluatedSection(a, b.source, b.mod)
        }
      )
    }
  }
  case class EvaluatedSection(section: Section, source: Source, mod: VorkModifier) {
    def out: String = section.statements.map(_.out).mkString
  }

  def fromClasspath(cp: String): MarkdownCompiler = {
    val prefix = if (cp.isEmpty) "" else cp + File.pathSeparator
    val runtimeOnly = defaultClasspath { path =>
      Set(
        "scala-library",
        "scala-reflect",
        "pprint",
        "vork-runtime"
      ).contains(path)
    }
    val finalRuntime =
      if (runtimeOnly.isEmpty) defaultClasspath
      else runtimeOnly
    new MarkdownCompiler(prefix + finalRuntime)
  }

  def default(): MarkdownCompiler = fromClasspath("")
  def render(
      sections: List[Input],
      reporter: Reporter,
      compiler: MarkdownCompiler
  ): EvaluatedDocument = {
    render(sections, compiler, reporter, "<input>")
  }

  def render(
      sections: List[Input],
      compiler: MarkdownCompiler,
      reporter: Reporter,
      filename: String
  ): EvaluatedDocument = {
    renderInputs(
      sections.map(s => SectionInput(s, dialects.Sbt1(s).parse[Source].get, VorkModifier.Default)),
      compiler,
      reporter,
      filename
    )
  }

  case class SectionInput(input: Input, source: Source, mod: VorkModifier)

  def renderInputs(
      sections: List[SectionInput],
      compiler: MarkdownCompiler,
      reporter: Reporter,
      filename: String
  ): EvaluatedDocument = {
    val instrumented = instrumentSections(sections)
    val doc = document(compiler, sections, instrumented, reporter, filename)
    val evaluated = EvaluatedDocument(doc, sections)
    evaluated
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
        if (sb.charAt(sb.size - 1) != '\n') {
          sb.append("\n")
        }

        statement.binders.foreach { binder =>
          section.mod match {
            case VorkModifier.Fail =>
              binder.value match {
                case CompileResult.TypecheckedOK(code, tpe, pos) =>
                  val mpos = Position
                    .Range(
                      doc.edit.originalInput,
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
            case VorkModifier.Default | VorkModifier.Passthrough =>
              statement.binders.foreach { binder =>
                sb.append(binder.name)
                  .append(": ")
                  .append(binder.tpe.render)
                  .append(" = ")
                  .append(pprint.PPrinter.BlackWhite.apply(binder.value))
                  .append("\n")
              }
            case c: VorkModifier.Custom =>
              throw new IllegalArgumentException(c.toString)
          }
        }
    }
    if (sb.nonEmpty && sb.last == '\n') sb.setLength(sb.length - 1)
    sb.toString()
  }

  def document(
      compiler: MarkdownCompiler,
      original: List[SectionInput],
      instrumented: String,
      reporter: Reporter,
      filename: String
  ): Document = {
    // Use string builder to avoid accidental stripMargin processing
    val wrapped = new StringBuilder()
      .append("package repl\n")
      .append("class Session extends _root_.vork.internal.document.DocumentBuilder {\n")
      .append("  def app(): Unit = {\n")
      .append(instrumented)
      .append("  }\n")
      .append("}\n")
      .toString()
    val instrumentedInput = InstrumentedInput(filename, wrapped)
    val compileInput = Input.VirtualFile(filename, wrapped)
    val edit = toTokenEdit(original.map(_.source), compileInput)
    compiler.compile(compileInput, reporter, edit) match {
      case Some(loader) =>
        val cls = loader.loadClass("repl.Session")
        val doc = cls.newInstance().asInstanceOf[DocumentBuilder].$doc
        try {
          doc.build(instrumentedInput)
        } catch {
          case e: PositionedException =>
            val input = original(e.section - 1).input
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

  // Copy paste from scalafix
  def defaultClasspath: String = defaultClasspath(_ => true)
  def defaultClasspath(filter: String => Boolean): String = {
    getClass.getClassLoader match {
      case u: URLClassLoader =>
        val paths = u.getURLs.iterator
          .map(sanitizeURL)
          .filter(path => filter(path))
          .toList
        paths.mkString(File.pathSeparator)
      case _ => ""
    }
  }

  def sanitizeURL(u: URL): String = {
    if (u.getProtocol.startsWith("bootstrap")) {
      import java.io._
      import java.nio.file._
      val stream = u.openStream
      val tmp = File.createTempFile("bootstrap-" + u.getPath, ".jar")
      Files
        .copy(stream, Paths.get(tmp.getAbsolutePath), StandardCopyOption.REPLACE_EXISTING)
      tmp.getAbsolutePath
    } else {
      URLDecoder.decode(u.getPath, "UTF-8")
    }
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
          case Some(b) if section.mod != VorkModifier.Fail =>
            b -> Patch.empty
          case _ =>
            val name = freshBinder()
            List(Term.Name(name)) -> ctx.addLeft(stat, s"${WIDTH_INDENT}val $name = \n")
        }
        val statPositions = position(stat.pos)
        val failPatch = section.mod match {
          case VorkModifier.Fail =>
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
      patch
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
            case Left(err) =>
              pprint.log(err)
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

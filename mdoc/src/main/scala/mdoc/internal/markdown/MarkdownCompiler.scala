package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import mdoc.Reporter
import mdoc.document.Document
import mdoc.document._
import mdoc.internal.document.DocumentBuilder
import mdoc.internal.pos.PositionSyntax
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.pos.TokenEditDistance
import scala.meta._
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.reflect.internal.util.AbstractFileClassLoader
import scala.reflect.internal.util.BatchSourceFile
import scala.reflect.internal.util.{Position => GPosition}
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.io.VirtualDirectory

object MarkdownCompiler {

  def default(): MarkdownCompiler = fromClasspath(classpath = "", scalacOptions = "")

  def buildDocument(
      compiler: MarkdownCompiler,
      reporter: Reporter,
      sectionInputs: List[SectionInput],
      instrumented: String,
      filename: String
  ): EvaluatedDocument = {
    // Use string builder to avoid accidental stripMargin processing
    val instrumentedInput = InstrumentedInput(filename, instrumented)
    val compileInput = Input.VirtualFile(filename, instrumented)
    val edit = TokenEditDistance.toTokenEdit(sectionInputs.map(_.source), compileInput)
    val doc = compiler.compile(compileInput, reporter, edit) match {
      case Some(loader) =>
        val cls = loader.loadClass("repl.Session$")
        val ctor = cls.getDeclaredConstructor()
        ctor.setAccessible(true)
        val doc = ctor.newInstance().asInstanceOf[DocumentBuilder].$doc
        try {
          doc.build(instrumentedInput)
        } catch {
          case e: PositionedException =>
            val input = sectionInputs(e.section).input
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
    EvaluatedDocument(doc, sectionInputs)
  }

  def fromClasspath(classpath: String, scalacOptions: String): MarkdownCompiler = {
    val fullClasspath =
      if (classpath.isEmpty) defaultClasspath(_ => true)
      else {
        val base = Classpath(classpath)
        val runtime = defaultClasspath(path => path.toString.contains("mdoc-runtime"))
        base ++ runtime
      }
    new MarkdownCompiler(fullClasspath.syntax, scalacOptions)
  }

  private def defaultClasspath(fn: Path => Boolean): Classpath = {
    val paths =
      getClass.getClassLoader
        .asInstanceOf[URLClassLoader]
        .getURLs
        .iterator
        .map(url => AbsolutePath(Paths.get(url.toURI)))
    Classpath(paths.toList)
  }

}

class MarkdownCompiler(
    classpath: String,
    scalacOptions: String,
    target: AbstractFile = new VirtualDirectory("(memory)", None)
) {
  private val settings = new Settings()
  settings.Yrangepos.value = true
  settings.deprecation.value = true // enable detailed deprecation warnings
  settings.unchecked.value = true // enable detailed unchecked warnings
  settings.outputDirs.setSingleOutput(target)
  settings.classpath.value = classpath
  settings.processArgumentString(scalacOptions)

  private val sreporter = new FilterStoreReporter(settings)
  private val global = new Global(settings, sreporter)
  private val appClasspath: Array[URL] = classpath
    .split(File.pathSeparator)
    .map(path => new File(path).toURI.toURL)
  private val appClassLoader = new URLClassLoader(
    appClasspath,
    this.getClass.getClassLoader
  )

  private def clearTarget(): Unit = target match {
    case vdir: VirtualDirectory => vdir.clear()
    case _ =>
  }

  private def toSource(input: Input): BatchSourceFile = {
    val filename = Paths.get(input.syntax).getFileName.toString
    new BatchSourceFile(filename, new String(input.chars))
  }

  def fail(original: Seq[Tree], input: Input): String = {
    sreporter.reset()
    val run = new global.Run
    run.compileSources(List(toSource(input)))
    val out = new ByteArrayOutputStream()
    val ps = new PrintStream(out)
    val edit = TokenEditDistance.toTokenEdit(original, input)
    sreporter.infos.foreach {
      case sreporter.Info(pos, msgOrNull, gseverity) =>
        val msg = nullableMessage(msgOrNull)
        val mpos = toMetaPosition(edit, pos)
        val severity = gseverity.toString().toLowerCase
        val formatted = PositionSyntax.formatMessage(mpos, severity, msg, includePath = false)
        ps.println(formatted)
    }
    out.toString()
  }

  def compile(input: Input, vreporter: Reporter, edit: TokenEditDistance): Option[ClassLoader] = {
    clearTarget()
    sreporter.reset()
    val run = new global.Run
    run.compileSources(List(toSource(input)))
    if (!sreporter.hasErrors) {
      Some(new AbstractFileClassLoader(target, appClassLoader))
    } else {
      report(vreporter, edit)
      None
    }
  }

  private def toMetaPosition(edit: TokenEditDistance, pos: GPosition): Position = {
    def toOffsetPosition(offset: Int): Position = {
      edit.toOriginal(offset) match {
        case Left(_) =>
          Position.None
        case Right(p) =>
          p.toUnslicedPosition
      }
    }
    if (pos.isDefined) {
      if (pos.isRange) {
        (edit.toOriginal(pos.start), edit.toOriginal(pos.end - 1)) match {
          case (Right(start), Right(end)) =>
            Position.Range(start.input, start.start, end.end).toUnslicedPosition
          case (_, _) =>
            toOffsetPosition(pos.point)
        }
      } else {
        toOffsetPosition(pos.point)
      }
    } else {
      Position.None
    }
  }

  private def nullableMessage(msgOrNull: String): String =
    if (msgOrNull == null) "" else msgOrNull
  private def report(vreporter: Reporter, edit: TokenEditDistance): Unit = {
    sreporter.infos.foreach {
      case sreporter.Info(pos, msgOrNull, severity) =>
        val msg = nullableMessage(msgOrNull)
        val mpos = toMetaPosition(edit, pos)
        severity match {
          case sreporter.ERROR => vreporter.error(mpos, msg)
          case sreporter.INFO => vreporter.info(mpos, msg)
          case sreporter.WARNING => vreporter.warning(mpos, msg)
        }
      case _ =>
    }
  }

}

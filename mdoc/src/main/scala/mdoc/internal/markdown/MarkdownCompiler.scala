package mdoc.internal.markdown

import java.io.File
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
import mdoc.Reporter
import mdoc.document.Document
import mdoc.document._
import mdoc.internal.document.DocumentBuilder
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.pos.TokenEditDistance

object MarkdownCompiler {

  def default(): MarkdownCompiler = fromClasspath("")

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
        val cls = loader.loadClass("repl.Session")
        val doc = cls.newInstance().asInstanceOf[DocumentBuilder].$doc
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

  def fromClasspath(classpath: String): MarkdownCompiler = {
    val fullClasspath =
      if (classpath.isEmpty) defaultClasspath(_ => true)
      else {
        val base = defaultClasspath(_ => true)
        val runtime = defaultClasspath(path => path.toString.contains("mdoc-runtime"))
        base ++ runtime
      }
    new MarkdownCompiler(fullClasspath.syntax)
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
      Some(new AbstractFileClassLoader(target, appClassLoader))
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

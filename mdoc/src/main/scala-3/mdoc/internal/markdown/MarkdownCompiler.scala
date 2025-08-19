package mdoc.internal.markdown

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URL
import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import sun.misc.Unsafe

import mdoc.Reporter
import mdoc.document.Document
import mdoc.document._
import mdoc.internal.document.DocumentBuilder
import mdoc.internal.document.MdocNonFatal
import mdoc.internal.pos.TokenEditDistance
import mdoc.internal.CompatClassloader
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.pos.PositionSyntax
import mdoc.internal.pos.PositionSyntax._

import scala.collection.JavaConverters._
import scala.collection.Seq
import scala.meta.Classpath
import scala.meta.AbsolutePath
import scala.meta.inputs.Input
import scala.meta.inputs.Position

import dotty.tools.dotc.Driver
import dotty.tools.dotc.core.Contexts.{Context, FreshContext}
import dotty.tools.dotc.config.Settings.Setting._
import dotty.tools.dotc.interfaces.SourcePosition
import dotty.tools.dotc.ast.Trees.Tree
import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.interfaces.{SourceFile => ISourceFile}
import dotty.tools.dotc.interfaces.{Diagnostic => IDiagnostic}
import dotty.tools.dotc.reporting._
import dotty.tools.dotc.parsing.Parsers.Parser
import dotty.tools.dotc.Compiler
import dotty.tools.io.{AbstractFile, VirtualDirectory}
import dotty.tools.repl.AbstractFileClassLoader
import dotty.tools.dotc.util.SourceFile

import scala.annotation.implicitNotFound

class MarkdownDriver(val settings: List[String]) extends Driver {

  /* Otherwise it will print usage instructions */
  override protected def sourcesRequired: Boolean = false

  def currentCtx = myInitCtx

  private val myInitCtx: Context = {
    val rootCtx = initCtx.fresh
    val ctx = setup(settings.toArray, rootCtx) match
      case Some((_, ctx)) => ctx
      case None => rootCtx
    ctx.initialize()(using ctx)
    ctx
  }
}

class MarkdownCompiler(
    classpath: String,
    val scalacOptions: String,
    target: AbstractFile = new VirtualDirectory("(memory)")
) {

  private val defaultFlags =
    List("-color:never", "-unchecked", "-deprecation", "-Ximport-suggestion-timeout", "0")
  private val defaultFlagSet = defaultFlags.filter(_.startsWith("-")).toSet

  private def newContext: FreshContext = {
    def removeDuplicatedOptions(options: List[String]): List[String] =
      options match
        case head :: next :: tail
            if defaultFlagSet.exists(flag => head.startsWith(flag)) && !next.startsWith("-") =>
          removeDuplicatedOptions(tail)
        case head :: tail if defaultFlagSet.exists(flag => head.startsWith(flag)) =>
          removeDuplicatedOptions(tail)
        case head :: tail => head :: removeDuplicatedOptions(tail)
        case Nil => Nil

    val options = removeDuplicatedOptions(scalacOptions.split("\\s+").filter(_.nonEmpty).toList)
    val settings =
      options ::: defaultFlags ::: "-classpath" :: classpath :: Nil

    val driver = new MarkdownDriver(settings)

    val ctx = driver.currentCtx.fresh

    ctx
      .setReporter(new CollectionReporter)
      .setSetting(
        ctx.settings.outputDir,
        target
      )
  }

  private var context = newContext

  def shutdown(): Unit = {}

  def classpathEntries: Seq[Path] =
    context.settings.classpath
      .value(using context)
      .split(File.pathSeparator)
      .map(url => Paths.get(url))

  private def reset(): Unit = {
    context = newContext
  }
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

  private def toSource(input: Input): SourceFile = {
    SourceFile.virtual(input.filename, new String(input.chars))
  }

  private def toInput(sourceFile: ISourceFile): Input = {
    Input.String(new String(sourceFile.content()))
  }

  def hasErrors: Boolean = context.reporter.hasErrors
  def hasWarnings: Boolean = context.reporter.hasWarnings

  def compileSources(
      input: Input,
      vreporter: Reporter,
      edit: TokenEditDistance,
      fileImports: List[FileImport]
  ): Unit = {
    reset()
    clearTarget()
    val compiler = new Compiler
    val run = compiler.newRun(using context)
    val inputs = List(input)
    val res = scala.util.Try(run.compileSources(inputs.map(toSource)))
    report(vreporter, input, fileImports, run.runContext, edit)
  }

  class CollectionReporter extends dotty.tools.dotc.reporting.Reporter with UniqueMessagePositions {
    val allDiags = List.newBuilder[Diagnostic]

    override def doReport(dia: Diagnostic)(using Context) =
      allDiags += dia

    override def pendingMessages(using Context) = allDiags.result()
  }

  def compile(
      input: Input,
      vreporter: Reporter,
      edit: TokenEditDistance,
      className: String,
      fileImports: List[FileImport],
      retry: Int = 0
  ): Option[Class[?]] = {
    compileSources(input, vreporter, edit, fileImports)
    if (!context.reporter.hasErrors) {
      val loader = new AbstractFileClassLoader(target, appClassLoader)
      try {
        Some(loader.loadClass(className))
      } catch {
        case _: ClassNotFoundException =>
          if (retry < 1) {
            reset()
            compile(input, vreporter, edit, className, fileImports, retry + 1)
          } else {
            vreporter.error(
              s"${input.syntax}: skipping file, the compiler produced no classfiles " +
                "and reported no errors to explain what went wrong during compilation. " +
                "Please report an issue to https://github.com/scalameta/mdoc/issues."
            )
            None
          }
      }
    } else {
      None
    }
  }

  def fail(edit: TokenEditDistance, input: Input, sectionPos: Position): String = {
    reset()
    val compiler = new Compiler
    val run = compiler.newRun(using context)
    val inputs = List(input).map(toSource)

    run.compileSources(inputs)
    val out = new ByteArrayOutputStream()
    val ps = new PrintStream(out)

    context.reporter.pendingMessages(using context).foreach { diagnostic =>
      val msg = nullableMessage(diagnostic.message)
      val mpos = toMetaPosition(edit, diagnostic.position.get)
      if (sectionPos.contains(mpos) || diagnostic.level == IDiagnostic.ERROR) {
        val severity = diagnostic.level match {
          case IDiagnostic.ERROR => "error"
          case IDiagnostic.WARNING => "warn"
          case IDiagnostic.INFO => "info"
        }
        val formatted =
          PositionSyntax.formatMessage(mpos, severity, "\n" + msg, includePath = false)
        ps.println(formatted)
      }
    }

    out.toString()
  }

  def toMetaPosition(edit: TokenEditDistance, pos: SourcePosition): Position = {
    def toOffsetPosition(offset: Int): Position = {
      edit.toOriginal(offset) match {
        case Left(_) => Position.None
        case Right(p) => p.toUnslicedPosition
      }
    }

    val start = pos.start
    val end = pos.end

    (edit.toOriginal(start), edit.toOriginal(end - 1)) match {
      case (Right(start), Right(end)) =>
        Position.Range(start.input, start.start, end.end).toUnslicedPosition
      case (_, _) =>
        toOffsetPosition(pos.point - 1)
    }

  }

  private def nullableMessage(msgOrNull: String): String =
    if (msgOrNull == null) "" else msgOrNull

  private def report(
      vreporter: Reporter,
      input: Input,
      fileImports: List[FileImport],
      context: Context,
      edit: TokenEditDistance
  ): Unit = {

    val infos = context.reporter.pendingMessages(using context).toSeq.sortBy(_.pos.source.path)
    infos.foreach {
      case diagnostic if diagnostic.position.isPresent =>
        val pos = diagnostic.position.get
        val msg = nullableMessage(diagnostic.message)

        val mpos = toMetaPosition(edit, pos)
        val actualMessage =
          if (mpos == Position.None) {
            val line = pos.lineContent
            if (line.nonEmpty) {
              formatMessage(pos, msg)
            } else {
              msg
            }
          } else {
            msg
          }
        reportMessage(vreporter, diagnostic, mpos, "\n" + actualMessage)
      case _ =>
    }
  }

  private def reportMessage(
      vreporter: Reporter,
      diagnostic: Diagnostic,
      mpos: Position,
      message: String
  ): Unit = {
    diagnostic match {
      case _: Diagnostic.Error => vreporter.error(mpos, message)
      case _: Diagnostic.Info => vreporter.info(mpos, message)
      case _: Diagnostic.Warning => vreporter.warning(mpos, message)
      case _ =>
    }
  }
  private def formatMessage(pos: SourcePosition, message: String): String =
    new CodeBuilder()
      .println(s"${pos.source().path()}:${pos.line + 1} (mdoc generated code) \n $message")
      .println(pos.lineContent)
      .toString

}

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

import scala.collection.JavaConverters._
import scala.collection.Seq
import scala.meta.Classpath
import scala.meta.AbsolutePath
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.meta.internal.inputs.XtensionInputSyntaxStructure

import dotty.tools.dotc.interactive.InteractiveDriver
import dotty.tools.dotc.interactive.Interactive
import dotty.tools.dotc.interactive.InteractiveCompiler
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.config.Settings.Setting._
import dotty.tools.dotc.interfaces.SourcePosition
import dotty.tools.dotc.ast.Trees.Tree
import dotty.tools.dotc.interfaces.{SourceFile => ISourceFile}
import dotty.tools.dotc.reporting.Diagnostic
import dotty.tools.dotc.parsing.Parsers.Parser
import dotty.tools.dotc.Compiler
import dotty.tools.io.{AbstractFile, VirtualDirectory}
import dotty.tools.repl.AbstractFileClassLoader
import dotty.tools.dotc.util.SourceFile

import scala.annotation.implicitNotFound

class MarkdownCompiler(
    classpath: String,
    val scalacOptions: String,
    target: AbstractFile = new VirtualDirectory("(memory)")
) {

  private def newDriver: InteractiveDriver = {
    val defaultFlags =
      List("-color:never", "-unchecked", "-deprecation", "-Ximport-suggestion-timeout", "0")
    val options = scalacOptions.split("\\s+").toList
    val settings =
      options ::: defaultFlags ::: "-classpath" :: classpath :: Nil
    new InteractiveDriver(settings.distinct)
  }
  private var driver = newDriver

  def shutdown(): Unit = {}

  def classpathEntries: Seq[Path] =
    driver.currentCtx.settings.classpath
      .value(using driver.currentCtx)
      .split(File.pathSeparator)
      .map(url => Paths.get(url))

  private def reset(): Unit = {
    driver = newDriver
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
    val filename = Paths.get(input.syntax).getFileName.toString
    SourceFile.virtual(filename, new String(input.chars))
  }

  private def toInput(sourceFile: ISourceFile): Input = {
    Input.String(new String(sourceFile.content()))
  }

  def hasErrors: Boolean = driver.currentCtx.reporter.hasErrors
  def hasWarnings: Boolean = driver.currentCtx.reporter.hasWarnings

  def compileSources(
      input: Input,
      vreporter: Reporter,
      edit: TokenEditDistance,
      fileImports: List[FileImport],
      context: Context
  ): Unit = {
    clearTarget()
    val compiler = new Compiler
    val run = compiler.newRun(using context)
    val inputs = List(input)
    scala.util.Try(run.compileSources(inputs.map(toSource)))
    report(vreporter, input, fileImports, run.runContext, edit)
  }

  def compile(
      input: Input,
      vreporter: Reporter,
      edit: TokenEditDistance,
      className: String,
      fileImports: List[FileImport],
      retry: Int = 0
  ): Option[Class[_]] = {
    reset()
    val context = driver.currentCtx.fresh.setSetting(
      driver.currentCtx.settings.outputDir,
      target
    )
    compileSources(input, vreporter, edit, fileImports, context)
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
    ???
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
    val infos = context.reporter.allErrors.toSeq.sortBy(_.pos.source.path)
    infos.foreach {
      case diagnostic if diagnostic.position.isPresent =>
        val pos = diagnostic.position.get
        val msg = nullableMessage(diagnostic.message)
        val mpos = Position.None //ParsedSource.toMetaPosition(edit, pos)
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
        reportMessage(vreporter, diagnostic, mpos, actualMessage)
      case _ =>
    }
  }

  private def reportMessage(
      vreporter: Reporter,
      diagnostic: Diagnostic,
      mpos: Position,
      message: String
  ): Unit = diagnostic match {
    case _: Diagnostic.Error => vreporter.error(mpos, message)
    case _: Diagnostic.Info => vreporter.info(mpos, message)
    case _: Diagnostic.Warning => vreporter.warning(mpos, message)
    case _ =>
  }
  private def formatMessage(pos: SourcePosition, message: String): String =
    new CodeBuilder()
      .println(s"${pos.source().path()}:${pos.line + 1} (mdoc generated code) $message")
      .println(pos.lineContent)
      .println(pos.point().toString)
      .toString

}

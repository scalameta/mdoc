package mdoc.internal.worksheets

import coursierapi.Logger
import mdoc.MainSettings
import mdoc.internal.cli.Context
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.MarkdownCompiler
import mdoc.internal.markdown.Modifier
import mdoc.internal.pos.PositionSyntax._
import mdoc.internal.worksheets.Compat._
import mdoc.{interfaces => i}

import java.io.File
import java.io.PrintStream
import java.nio.file.Path
import java.{util => ju}
import scala.meta.inputs.Input
import scala.meta.internal.io.PathIO

class Mdoc(settings: MainSettings) extends i.Mdoc {

  private var myContext: Context = null

  def this() = this(MainSettings())

  def withWorkingDirectory(cwd: Path): i.Mdoc =
    new Mdoc(this.settings.withWorkingDirectory(cwd))
  def withClasspath(classpath: ju.List[Path]): i.Mdoc =
    new Mdoc(this.settings.withClasspath(classpath.iterator().asScala.mkString(File.pathSeparator)))
  def withScalacOptions(options: ju.List[String]): i.Mdoc =
    new Mdoc(this.settings.withScalacOptions(options.iterator().asScala.mkString(" ")))
  def withSettings(settings: ju.List[String]): i.Mdoc =
    new Mdoc(this.settings.withArgs(settings.iterator().asScala.toList))
  def withConsoleReporter(out: PrintStream): i.Mdoc =
    new Mdoc(this.settings.withReporter(new ConsoleReporter(out)))
  def withScreenWidth(screenWidth: Int): i.Mdoc =
    new Mdoc(this.settings.withScreenWidth(screenWidth))
  def withScreenHeight(screenHeight: Int): i.Mdoc =
    new Mdoc(this.settings.withScreenHeight(screenHeight))
  def withCoursierLogger(logger: Logger): mdoc.interfaces.Mdoc =
    new Mdoc(this.settings.withCoursierLogger(logger))

  def shutdown(): Unit = {
    if (myContext != null) {
      myContext.compiler.shutdown()
      usedDummy()
    }
  }

  def evaluateWorksheet(filename: String, text: String): i.EvaluatedWorksheet = {
    new WorksheetProvider(settings.settings).evaluateWorksheet(
      Input.VirtualFile(filename, text),
      context(),
      modifier = None
    )
  }

  def evaluateWorksheet(filename: String, text: String, modifier: String): i.EvaluatedWorksheet =
    new WorksheetProvider(settings.settings).evaluateWorksheet(
      Input.VirtualFile(filename, text),
      context(),
      Modifier(modifier)
    )

  private def context(): Context = {
    if (myContext == null) {
      myContext = Context.fromOptions(settings.settings, settings.reporter)
    }
    myContext
  }
}

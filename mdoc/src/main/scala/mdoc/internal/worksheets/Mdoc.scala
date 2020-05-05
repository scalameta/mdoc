package mdoc.internal.worksheets

import java.{util => ju}
import java.io.File
import java.io.PrintStream
import java.nio.file.Path
import scala.collection.JavaConverters._
import mdoc.{interfaces => i}
import mdoc.internal.cli.Context
import mdoc.internal.cli.Settings
import scala.meta.internal.io.PathIO
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.MarkdownCompiler
import scala.meta.inputs.Input
import mdoc.internal.worksheets.Compat._
import mdoc.MainSettings
import coursierapi.Logger

class Mdoc(settings: MainSettings) extends i.Mdoc {

  private var myContext: Context = null

  def this() = this(MainSettings())

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
      myContext.compiler.global.close()
      usedDummy()
    }
  }

  def evaluateWorksheet(filename: String, text: String): EvaluatedWorksheet =
    new WorksheetProvider(settings.settings).evaluateWorksheet(
      Input.VirtualFile(filename, text),
      context
    )

  private def context(): Context = {
    if (myContext == null) {
      myContext = Context.fromOptions(settings.settings, settings.reporter)
    }
    myContext
  }
}

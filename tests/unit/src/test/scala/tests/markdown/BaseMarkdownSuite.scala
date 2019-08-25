package tests.markdown

import com.vladsch.flexmark.util.options.MutableDataSet
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import mdoc.Reporter
import mdoc.internal.cli.Context
import mdoc.internal.cli.MdocProperties
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.Markdown
import mdoc.internal.markdown.MarkdownCompiler
import scala.meta.inputs.Input
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.meta.testkit.DiffAssertions
import tests.markdown.StringSyntax._
import mdoc.internal.pos.PositionSyntax._

abstract class BaseMarkdownSuite extends org.scalatest.FunSuite with DiffAssertions {
  def createTempDirectory(): AbsolutePath = {
    val dir = AbsolutePath(Files.createTempDirectory("mdoc"))
    dir.toFile.deleteOnExit()
    dir
  }
  def createTempFile(filename: String): AbsolutePath = {
    val file = createTempDirectory().resolve(filename)
    file.write("")
    file
  }
  protected def baseSettings: Settings =
    Settings
      .default(createTempDirectory())
      .copy(
        site = Map(
          "version" -> "1.0"
        )
      )
      .withProperties(MdocProperties.default(PathIO.workingDirectory))

  def postProcessObtained: Map[String, String => String] = Map.empty
  def postProcessExpected: Map[String, String => String] = Map.empty
  private val myStdout = new ByteArrayOutputStream()
  private def newReporter(): ConsoleReporter = {
    new ConsoleReporter(new PrintStream(myStdout))
  }
  protected def scalacOptions: String = ""
  private val compiler = MarkdownCompiler.fromClasspath("", scalacOptions)
  private def newContext(settings: Settings, reporter: ConsoleReporter) = {
    settings.validate(reporter)
    if (reporter.hasErrors) fail()
    Context(settings, reporter, compiler)
  }

  def getMarkdownSettings(context: Context): MutableDataSet = {
    myStdout.reset()
    Markdown.mdocSettings(context)
  }

  def checkError(
      name: String,
      original: String,
      expected: String,
      settings: Settings = baseSettings,
      compat: Map[String, String] = Map.empty
  ): Unit = {
    test(name) {
      val reporter = newReporter()
      val context = newContext(settings, reporter)
      val input = Input.VirtualFile(name + ".md", original)
      Markdown.toMarkdown(input, getMarkdownSettings(context), reporter, settings)
      assert(reporter.hasErrors, "Expected errors but reporter.hasErrors=false")
      val obtainedErrors = Compat.postProcess(
        fansi.Str(myStdout.toString).plainText.trimLineEnds,
        postProcessObtained
      )
      assertNoDiffOrPrintExpected(obtainedErrors, Compat(expected, compat, postProcessExpected))
    }
  }

  def checkCompiles(
      name: String,
      original: String,
      settings: Settings = baseSettings,
      onOutput: String => Unit = _ => ()
  ): Unit = {
    test(name) {
      val reporter = newReporter()
      val context = newContext(settings, reporter)
      val input = Input.VirtualFile(name + ".md", original)
      val obtained =
        Markdown.toMarkdown(input, getMarkdownSettings(context), reporter, settings).trimLineEnds
      val colorOut = myStdout.toString()
      print(colorOut)
      val stdout = fansi.Str(colorOut).plainText
      assert(!reporter.hasErrors, stdout)
      assert(!reporter.hasWarnings, stdout)
      onOutput(obtained)
    }
  }

  def check(
      name: String,
      original: String,
      expected: String,
      settings: Settings = baseSettings
  ): Unit = {
    checkCompiles(name, original, settings, obtained => {
      assertNoDiffOrPrintExpected(obtained, Compat(expected, Map.empty))
    })
  }

}

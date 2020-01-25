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
import tests.markdown.StringSyntax._
import mdoc.internal.pos.PositionSyntax._
import scala.meta.io.RelativePath
import munit.TestOptions

abstract class BaseMarkdownSuite extends munit.FunSuite {
  override def munitFlakyOK = true
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
    myStdout.reset()
    new ConsoleReporter(new PrintStream(myStdout))
  }
  protected def scalacOptions: String = ""
  private val compiler = MarkdownCompiler.fromClasspath("", scalacOptions)
  private def newContext(settings: Settings, reporter: ConsoleReporter) = {
    settings.validate(reporter)
    if (reporter.hasErrors) fail("reporter has errors")
    Context(settings, reporter, compiler)
  }

  def checkError(
      name: TestOptions,
      original: String,
      expected: String,
      settings: Settings = baseSettings,
      compat: Map[String, String] = Map.empty
  ): Unit = {
    test(name) {
      val reporter = newReporter()
      val context = newContext(settings, reporter)
      val input = Input.VirtualFile(name.name + ".md", original)
      val relpath = RelativePath(input.path)
      Markdown.toMarkdown(input, context, relpath, baseSettings.site, reporter, settings)
      assert(reporter.hasErrors, "Expected errors but reporter.hasErrors=false")
      val obtainedErrors = Compat.postProcess(
        fansi.Str(myStdout.toString).plainText.trimLineEnds,
        postProcessObtained
      )
      assertNoDiff(
        Compat(obtainedErrors, compat, postProcessObtained),
        Compat(expected, compat, postProcessExpected)
      )
    }
  }

  def checkCompiles(
      name: TestOptions,
      original: String,
      settings: Settings = baseSettings,
      onOutput: String => Unit = _ => ()
  ): Unit = {
    test(name) {
      val reporter = newReporter()
      val context = newContext(settings, reporter)
      val input = Input.VirtualFile(name.name + ".md", original)
      val relpath = RelativePath(input.path)
      val obtained =
        Markdown.toMarkdown(input, context, relpath, baseSettings.site, reporter, settings)
      val colorOut = myStdout.toString()
      print(colorOut)
      val stdout = fansi.Str(colorOut).plainText
      assert(!reporter.hasErrors, stdout)
      assert(!reporter.hasWarnings, stdout)
      onOutput(obtained)
    }
  }

  def check(
      name: TestOptions,
      original: String,
      expected: String,
      settings: Settings = baseSettings
  ): Unit = {
    checkCompiles(name, original, settings, obtained => {
      assertNoDiff(obtained, Compat(expected, Map.empty))
    })
  }

}

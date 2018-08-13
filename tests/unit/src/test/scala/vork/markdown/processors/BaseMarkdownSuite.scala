package vork.markdown.processors

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import scala.meta.testkit.DiffAssertions
import com.vladsch.flexmark.util.options.MutableDataSet
import java.io.PrintStream
import scala.meta.io.AbsolutePath
import vork.Context
import vork.Logger
import vork.Markdown
import vork.Args
import vork.Processor
import StringSyntax._
import scala.meta.inputs.Input

abstract class BaseMarkdownSuite extends org.scalatest.FunSuite with DiffAssertions {
  private val tmp = AbsolutePath(Files.createTempDirectory("vork"))
  private val options = Args
    .default(tmp)
    .copy(
      site = Map(
        "version" -> "1.0"
      )
    )
  private val myStdout = new ByteArrayOutputStream()
  private val logger = new Logger(new PrintStream(myStdout))
  private val compiler = MarkdownCompiler.fromClasspath(options.classpath)
  private val context = Context(options, logger, compiler)

  def getSettings(input: Input.VirtualFile): MutableDataSet = {
    myStdout.reset()
    val settings = Markdown.default(context)
    settings.set(Processor.InputKey, Some(input))
    settings
  }

  def checkError(
      name: String,
      original: String,
      expected: String
  ): Unit = {
    test(name) {
      val input = Input.VirtualFile(name + ".md", original)
      Markdown.toMarkdown(original, getSettings(input))
      assert(logger.hasErrors, "Expected errors but logger.hasErrors=false")
      val obtainedErrors = fansi.Str(myStdout.toString).plainText.trimLineEnds
      assertNoDiff(obtainedErrors, expected)
    }
  }

  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      val input = Input.VirtualFile(name + ".md", original)
      val obtained = Markdown.toMarkdown(original, getSettings(input)).trimLineEnds
      assertNoDiff(obtained, expected)
    }
  }
}

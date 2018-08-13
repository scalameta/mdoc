package vork.markdown.processors

import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.meta.testkit.DiffAssertions
import com.vladsch.flexmark.util.options.MutableDataSet
import java.io.PrintStream
import org.langmeta.io.AbsolutePath
import vork.Context
import vork.Logger
import vork.Markdown
import vork.Args
import vork.Processor

abstract class BaseMarkdownSuite extends org.scalatest.FunSuite with DiffAssertions {
  private val tmp = AbsolutePath(Files.createTempDirectory("vork"))
  private val options = Args.default(tmp)
  private val myStdout = new ByteArrayOutputStream()
  private val logger = new Logger(new PrintStream(myStdout))
  private val compiler = MarkdownCompiler.fromClasspath(options.classpath)
  private val context = Context(options, logger, compiler)

  def getSettings(name: String): MutableDataSet = {
    myStdout.reset()
    val settings = Markdown.default(context)
    settings.set(Processor.PathKey, Some(Paths.get(name + ".md")))
    settings
  }

  def checkError(
      name: String,
      original: String,
      expected: String
  ): Unit = {
    test(name) {
      Markdown.toMarkdown(original, getSettings(name))
      assert(logger.hasErrors, "Expected errors but logger.hasErrors=false")
      val obtainedErrors = fansi.Str(myStdout.toString).plainText
      assertNoDiff(obtainedErrors, expected)
    }
  }

  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      val obtained = Markdown.toMarkdown(original, getSettings(name))
      // println(obtained)
      assertNoDiff(obtained, expected)
    }
  }
}

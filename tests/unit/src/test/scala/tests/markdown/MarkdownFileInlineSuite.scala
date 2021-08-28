package tests.markdown

import munit.FunSuite
import mdoc.internal.markdown.MarkdownFile
import scala.meta.inputs.Input
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.InlineCode
import mdoc.internal.markdown.InlineMdoc
import mdoc.internal.markdown.Text
import mdoc.internal.markdown.MarkdownPart
import mdoc.internal.markdown.CodeFence
import scala.meta.io.RelativePath
import mdoc.internal.cli.InputFile
import scala.meta.io.AbsolutePath
import java.nio.file.Files
import mdoc.internal.cli.Settings
import scala.meta.internal.io.PathIO

class MarkdownFileInlineSuite extends FunSuite {
  val reporter = new ConsoleReporter(System.out)

  def check(name: String, original: String, expected: MarkdownPart*): Unit = {
    test(name) {
      reporter.reset()
      val input = Input.VirtualFile(name, original)
      val file = InputFile.fromRelativeFilename(name, Settings.default(PathIO.workingDirectory))
      val obtained = MarkdownFile.parse(input, file, reporter).parts
      require(!reporter.hasErrors)
      val expectedParts = expected.toList
      assertNoDiff(
        pprint.tokenize(obtained).mkString,
        pprint.tokenize(expectedParts).mkString
      )
    }
  }

  check(
    "inlineSmall",
    """Hello `scala mdoc println(42)` World""".stripMargin,
    Text("Hello "),
    InlineMdoc(Text("scala mdoc"), Text("println(42)")),
    Text(" World"),
  )

  check(
    "inlineCrash",
    """Hello `scala mdoc:crash println(42)` World""".stripMargin,
    Text("Hello "),
    InlineMdoc(Text("scala mdoc:crash"), Text("println(42)")),
    Text(" World"),
  )

  check(
    "inlineIgnoreNonMdoc",
    """Hello `println("Unevaluated code")` World""".stripMargin,
    Text("Hello "),
    InlineCode(Text("""`println("Unevaluated code")`""")),
    Text(" World"),
  )

}

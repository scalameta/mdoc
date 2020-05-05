package tests.markdown

import munit.FunSuite
import mdoc.internal.markdown.MarkdownFile
import scala.meta.inputs.Input
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.Text
import mdoc.internal.markdown.MarkdownPart
import mdoc.internal.markdown.CodeFence
import scala.meta.io.RelativePath
import mdoc.internal.cli.InputFile
import scala.meta.io.AbsolutePath
import java.nio.file.Files
import mdoc.internal.cli.Settings
import scala.meta.internal.io.PathIO

class MarkdownFileSuite extends FunSuite {
  val reporter = new ConsoleReporter(System.out)

  def check(name: String, original: String, expected: MarkdownPart*): Unit = {
    test(name) {
      reporter.reset()
      val input = Input.VirtualFile(name, original)
      val file = InputFile.fromSettings(name, Settings.default(PathIO.workingDirectory))
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
    "basic",
    """# Hello
      |World
      |```scala mdoc
      |println(42)
      |```
      |End.
      |""".stripMargin,
    Text("# Hello\n"),
    Text("World\n"),
    CodeFence(
      Text("```"),
      Text("scala mdoc\n"),
      Text("println(42)"),
      Text("\n```\n")
    ),
    Text("End.\n")
  )

  check(
    "four-backtick",
    """# Hello
      |World
      |````scala mdoc
      |```
      |println(42)
      |```
      |````
      |End.
      |""".stripMargin,
    Text("# Hello\n"),
    Text("World\n"),
    CodeFence(
      Text("````"),
      Text("scala mdoc\n"),
      Text("```\nprintln(42)\n```"),
      Text("\n````\n")
    ),
    Text("End.\n")
  )

  check(
    "two-backtick",
    """# Hello
      |World
      |``scala mdoc
      |println(42)
      |``
      |End.
      |""".stripMargin,
    Text("# Hello\n"),
    Text("World\n"),
    Text("``scala mdoc\n"),
    Text("println(42)\n"),
    Text("``\n"),
    Text("End.\n")
  )

  check(
    "backtick-mismatch",
    """|````scala mdoc
       |`````
       |````
       |42
       |""".stripMargin,
    CodeFence(
      Text("````"),
      Text("scala mdoc\n"),
      Text(""),
      Text("\n`````\n")
    ),
    CodeFence(
      Text("````"),
      Text("\n"),
      Text("42"),
      Text("\n")
    )
  )
}

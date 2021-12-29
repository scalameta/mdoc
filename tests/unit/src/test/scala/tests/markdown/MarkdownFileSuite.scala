package tests.markdown

import munit.{FunSuite, Location}
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

  def checkParse(name: String, original: String, expected: MarkdownPart*)(implicit
      loc: Location
  ): Unit = {
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

  def checkRenderToString(name: String, original: MarkdownPart, expected: String)(implicit
      loc: Location
  ): Unit = {
    test(name) {
      val sb = new StringBuilder
      original.renderToString(sb)
      assertNoDiff(
        sb.toString,
        expected
      )
    }
  }

  checkParse(
    "parse. basic",
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

  checkParse(
    "parse. four-backtick",
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

  checkParse(
    "parse. two-backtick",
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

  checkParse(
    "parse. backtick-mismatch",
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

  checkParse(
    "parse. indented with whitespaces",
    """prefix
      |  ```scala mdoc
      |  println("foo")
      |  println("bar")
      |  ```
      |suffix
      |""".stripMargin,
    Text("prefix\n"),
    CodeFence(
      Text("```"),
      Text("scala mdoc\n"),
      Text("println(\"foo\")\nprintln(\"bar\")"),
      Text("\n```\n"),
      Text("  ")
    ),
    Text("suffix\n")
  )

  checkParse(
    "parse. indented with tag",
    """prefix
      |: ```scala mdoc
      |  println(42)
      |  ```
      |suffix
      |""".stripMargin,
    Text("prefix\n"),
    CodeFence(
      Text("```"),
      Text("scala mdoc\n"),
      Text("println(42)"),
      Text("\n```\n"),
      Text(": ")
    ),
    Text("suffix\n")
  )

  checkRenderToString(
    "render. basic",
    CodeFence(
      Text("```"),
      Text("scala mdoc\n"),
      Text("println(42)"),
      Text("\n```\n")
    ),
    """```scala mdoc
      |println(42)
      |```
      |""".stripMargin
  )

  checkRenderToString(
    "render. indented, one-liner body",
    CodeFence(
      Text("```"),
      Text("scala mdoc\n"),
      Text("println(42)"),
      Text("\n```\n"),
      Text("  ")
    ),
    """  ```scala mdoc
      |  println(42)
      |  ```
      |""".stripMargin
  )

  checkRenderToString(
    "render. indented, multi-line body",
    CodeFence(
      Text("```"),
      Text("scala mdoc\n"),
      Text("println(42)\nprintln(52)"),
      Text("\n```\n"),
      Text("  ")
    ),
    """  ```scala mdoc
      |  println(42)
      |  println(52)
      |  ```
      |""".stripMargin
  )

  checkRenderToString(
    "render. indented with tag, multi-line body",
    CodeFence(
      Text("```"),
      Text("scala mdoc\n"),
      Text("println(42)\nprintln(52)"),
      Text("\n```\n"),
      Text(":   ")
    ),
    """:   ```scala mdoc
      |    println(42)
      |    println(52)
      |    ```
      |""".stripMargin
  )
}

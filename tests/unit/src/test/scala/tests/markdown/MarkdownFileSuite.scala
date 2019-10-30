package tests.markdown

import org.scalatest.FunSuite
import scala.meta.testkit.DiffAssertions
import mdoc.internal.markdown.MarkdownFile
import scala.meta.inputs.Input
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.Text
import mdoc.internal.markdown.MarkdownPart
import mdoc.internal.markdown.CodeFence
import utest._
import scala.meta.io.RelativePath

object Assertions extends DiffAssertions

object MarkdownFileSuite extends TestSuite {
  import Assertions._
  val reporter = new ConsoleReporter(System.out)

  def check(original: String, expected: MarkdownPart*)(
      implicit path: utest.framework.TestPath
  ): Unit = {
    reporter.reset()
    val input = Input.VirtualFile(path.value.mkString("."), original)
    val relpath = RelativePath(input.path)
    val obtained = MarkdownFile.parse(input, relpath, reporter).parts
    require(!reporter.hasErrors)
    val expectedParts = expected.toList
    assertNoDiff(
      pprint.tokenize(obtained).mkString,
      pprint.tokenize(expectedParts).mkString
    )
  }
  val tests = Tests {

    test("basic") {
      check(
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
    }

    test("four-backtick") {
      check(
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
    }

    test("two-backtick") {
      check(
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
    }

    test("backtick-mismatch") {
      check(
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
  }
}

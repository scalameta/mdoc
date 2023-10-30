package tests.parser

import munit.{FunSuite, Location}

import mdoc.parser._

class MarkdownPartSuite extends tests.BaseSuite {
  private val parserSettings = new ParserSettings {
    val allowCodeFenceIndented: Boolean = true
  }

  def checkParse(name: String, original: String, expected: MarkdownPart*)(implicit
      loc: Location
  ): Unit = {
    test(name) {
      val obtained = MarkdownPart.parse(original, parserSettings)
      val expectedParts = expected.toList
      assertNoDiff(
        pprint.tokenize(obtained).mkString,
        pprint.tokenize(expectedParts).mkString
      )
      assertEquals(expected.length, obtained.length)
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

package tests.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import munit.FunSuite
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.DocumentLinks
import mdoc.internal.markdown.LinkHygiene
import mdoc.internal.markdown.DeadLinkInfo
import scala.meta.inputs.Position
import scala.meta.inputs.Input

class LinkHygieneSuite extends FunSuite {
  def check(
      name: String,
      original: String,
      expected: List[DeadLinkInfo],
      verbose: Boolean = false
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      val root = tests.cli.StringFS.fromString(original)
      val settings = Settings
        .default(root)
        .copy(reportRelativePaths = true, in = List(root), out = List(root))
      val links = DocumentLinks.fromGeneratedSite(settings)
      val obtained = LinkHygiene.lint(links, verbose)
      assertEquals(obtained, expected)
    }
  }
  val testContent1 =
    s"""|# Section
        |
        |Error [link](#does-not-exist) failed.
        |Typo [section](#sectionn) failed.
        |
        |## Sub-section
        |
        |<a id="id"></a>
        |<a name="name"></a>
        |
        |Internal:
        |* [section](#section)
        |* [sub](#sub-section)
        |* [id](#id)å
        |* [name](#name)
        |
        |Explicit file path:
        |* [section](a.md#section)
        |* [sub](a.md#sub-section)
        |* [id](a.md#id)
        |* [name](a.md#name)
        |""".stripMargin
  check(
    "single-file",
    s"""
       |/a.md
       |$testContent1""".stripMargin,
    List(
      DeadLinkInfo(
        Position.Range(Input.VirtualFile("a.md", testContent1), 17, 40),
        "Unknown link 'a.md#does-not-exist'."
      ),
      DeadLinkInfo(
        Position.Range(Input.VirtualFile("a.md", testContent1), 54, 74),
        "Unknown link 'a.md#sectionn', did you mean 'a.md#section'?"
      )
    )
  )

  check(
    "two-files",
    """
      |/a.md
      |# A
      |[b](b.md#b)
      |/b.md
      |# B
      |[a](a.md#a)
      |
    """.stripMargin,
    List.empty
  )

  check(
    "nested-directories",
    """
      |/a/a.md
      |[b](../b/b.md#b)
      |[i](../index.md)
      |/index.md
      |Index
      |/b/b.md
      |# B
    """.stripMargin,
    List.empty
  )

  check(
    "img",
    """
      |/a.md
      |![i](/static/logo.png)
    """.stripMargin,
    List.empty
  )

  val testContent2 = "[absolute](/absolute.md)"
  check(
    "absolute",
    s"""
       |/a.md
       |$testContent2""".stripMargin,
    List(
      DeadLinkInfo(
        Position.Range(Input.VirtualFile("a.md", testContent2), 0, 24),
        "Unknown link '/absolute.md', did you mean 'a.md'? To fix this problem, either make the link relative or turn it into complete URL such as http://example.com/absolute.md."
      )
    )
  )

  val testContent3 =
    """|# Header 1
       |[2](b.md#header)
       |""".stripMargin
  check(
    "verbose",
    s"""
       |/a.md
       |$testContent3
       |/b.md
       |# Header 2
    """.stripMargin,
    List(
      DeadLinkInfo(
        Position.Range(Input.VirtualFile("a.md", testContent3), 11, 27),
        """|Unknown link 'b.md#header', did you mean 'b.md#header-2'?
           |isValidHeading:
           |  a.md
           |  a.md#header-1
           |  b.md
           |  b.md#header-2""".stripMargin
      )
    ),
    verbose = true
  )

  test("console rendering") {
    val myOut = new ByteArrayOutputStream()
    val reporter = new ConsoleReporter(new PrintStream(myOut))
    val original =
      s"""|/a.md
          |$testContent1""".stripMargin
    myOut.reset()
    reporter.reset()
    val root = tests.cli.StringFS.fromString(original)
    val settings = Settings
      .default(root)
      .copy(reportRelativePaths = true, in = List(root), out = List(root))
    val links = DocumentLinks.fromGeneratedSite(settings)
    val result = LinkHygiene.lint(links, false)
    val resultVerbose = LinkHygiene.lint(links, true)
    LinkHygiene.report(asError = true, result, reporter)
    LinkHygiene.report(asError = false, result, reporter)
    val obtained = fansi.Str(myOut.toString()).plainText
    val expected =
      """|error: a.md:3:7: Unknown link 'a.md#does-not-exist'.
         |Error [link](#does-not-exist) failed.
         |      ^^^^^^^^^^^^^^^^^^^^^^^
         |error: a.md:4:6: Unknown link 'a.md#sectionn', did you mean 'a.md#section'?
         |Typo [section](#sectionn) failed.
         |     ^^^^^^^^^^^^^^^^^^^^
         |warning: a.md:3:7: Unknown link 'a.md#does-not-exist'.
         |Error [link](#does-not-exist) failed.
         |      ^^^^^^^^^^^^^^^^^^^^^^^
         |warning: a.md:4:6: Unknown link 'a.md#sectionn', did you mean 'a.md#section'?
         |Typo [section](#sectionn) failed.
         |     ^^^^^^^^^^^^^^^^^^^^""".stripMargin
    assertNoDiff(obtained, expected)
  }

}

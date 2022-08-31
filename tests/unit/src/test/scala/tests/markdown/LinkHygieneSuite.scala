package tests.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import munit.FunSuite
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.DocumentLinks
import mdoc.internal.markdown.LinkHygiene

class LinkHygieneSuite extends FunSuite {
  private val myOut = new ByteArrayOutputStream()
  private val reporter = new ConsoleReporter(new PrintStream(myOut))
  def check(
      name: String,
      original: String,
      expected: String,
      verbose: Boolean = false
  )(implicit loc: munit.Location): Unit = {
    test(name) {
      myOut.reset()
      reporter.reset()
      val root = tests.cli.StringFS.fromString(original)
      val settings = Settings
        .default(root)
        .copy(reportRelativePaths = true, in = List(root), out = List(root))
      val links = DocumentLinks.fromGeneratedSite(settings, reporter)
      LinkHygiene.lint(links, reporter, verbose)
      val obtained = fansi.Str(myOut.toString()).plainText
      assertNoDiff(obtained, expected)
    }
  }

  check(
    "single-file",
    """
      |/a.md
      |# Section
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
      |* [id](#id)
      |* [name](#name)
      |
      |Explicit file path:
      |* [section](a.md#section)
      |* [sub](a.md#sub-section)
      |* [id](a.md#id)
      |* [name](a.md#name)
      |
    """.stripMargin,
    """|warning: a.md:3:7: Unknown link 'a.md#does-not-exist'.
       |Error [link](#does-not-exist) failed.
       |      ^^^^^^^^^^^^^^^^^^^^^^^
       |warning: a.md:4:6: Unknown link 'a.md#sectionn', did you mean 'a.md#section'?
       |Typo [section](#sectionn) failed.
       |     ^^^^^^^^^^^^^^^^^^^^
    """.stripMargin
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
    ""
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
    ""
  )

  check(
    "img",
    """
      |/a.md
      |![i](/static/logo.png)
    """.stripMargin,
    ""
  )

  check(
    "absolute",
    """
      |/a.md
      |[absolute](/absolute.md)
    """.stripMargin,
    """|warning: a.md:1:1: Unknown link '/absolute.md', did you mean 'a.md'? To fix this problem, either make the link relative or turn it into complete URL such as http://example.com/absolute.md.
       |[absolute](/absolute.md)
       |^^^^^^^^^^^^^^^^^^^^^^^^
       |""".stripMargin
  )

  check(
    "verbose",
    """
      |/a.md
      |# Header 1
      |[2](b.md#header)
      |/b.md
      |# Header 2
    """.stripMargin,
    """|warning: a.md:2:1: Unknown link 'b.md#header', did you mean 'b.md#header-2'?
       |isValidHeading:
       |  a.md
       |  a.md#header-1
       |  b.md
       |  b.md#header-2
       |[2](b.md#header)
       |^^^^^^^^^^^^^^^^
       |""".stripMargin,
    verbose = true
  )

}

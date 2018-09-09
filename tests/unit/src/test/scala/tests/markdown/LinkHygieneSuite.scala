package tests.markdown

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.scalatest.FunSuite
import scala.meta.testkit.DiffAssertions
import scala.meta.testkit.StringFS
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.DocumentLinks
import mdoc.internal.markdown.LinkHygiene

class LinkHygieneSuite extends FunSuite with DiffAssertions {
  private val myOut = new ByteArrayOutputStream()
  private val reporter = new ConsoleReporter(new PrintStream(myOut))
  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      myOut.reset()
      reporter.reset()
      val root = StringFS.fromString(original)
      val settings = Settings
        .default(root)
        .copy(reportRelativePaths = true, in = root, out = root)
      val links = DocumentLinks.fromGeneratedSite(settings, reporter)
      LinkHygiene.lint(links, reporter)
      val obtained = fansi.Str(myOut.toString()).plainText
      assertNoDiffOrPrintExpected(obtained, expected)
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
    """|warning: a.md:3:7: warning: Reference 'a.md#does-not-exist' does not exist
       |Error [link](#does-not-exist) failed.
       |      ^^^^^^^^^^^^^^^^^^^^^^^
       |warning: a.md:4:6: warning: Reference 'a.md#sectionn' does not exist
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
    """|warning: a.md:1:1: warning: Reference '/absolute.md' does not exist. To fix this problem, make the link relative.
       |[absolute](/absolute.md)
       |^^^^^^^^^^^^^^^^^^^^^^^^
    """.stripMargin
  )

}

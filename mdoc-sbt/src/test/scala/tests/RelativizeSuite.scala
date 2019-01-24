package tests

import java.nio.file.Paths
import org.scalatest.FunSuite
import sbtdocusaurus.internal.Relativize
import scala.meta.internal.io.PathIO
import scala.meta.testkit.DiffAssertions
import scala.meta.testkit.StringFS

class RelativizeSuite extends FunSuite with DiffAssertions {

  def check(name: String, original: String, expected: String): Unit = {
    test(name) {
      val root = StringFS.fromString(original)
      Relativize.htmlSite(root.toNIO)
      root.toRelative(PathIO.workingDirectory).toURI(true)
      val isTrivial = Set(
        "<html>",
        "<body>",
        "<head>",
        "</head>",
        "<head></head>",
        "<body>",
        "</body>",
        "<body></body>",
        "</html>",
      )
      val obtained = StringFS
        .asString(root)
        .linesIterator
        .map(_.trim)
        .filterNot(isTrivial)
        .mkString("\n")
      assertNoDiff(obtained, expected)
    }
  }

  check(
    "a:href is processed",
    """
      |/index.html
      |<a href="/docs/about.html"></a>
      |""".stripMargin,
    """
      |/index.html
      |<a href="docs/about.html"></a>
      |""".stripMargin,
  )

  check(
    "img:src is processed",
    """
      |/index.html
      |<img src="/docs/about.html">
      |""".stripMargin,
    """
      |/index.html
      |<img src="docs/about.html">
      |""".stripMargin,
  )

  check(
    "script:src is processed",
    """
      |/index.html
      |<script src="/myscript.js"></script>
      |""".stripMargin,
    """
      |/index.html
      |<script src="myscript.js"></script>
      |""".stripMargin,
  )

  check(
    "link:href is processed",
    """
      |/index.html
      |<link href="/docs/about.html">
      |""".stripMargin,
    """
      |/index.html
      |<link href="docs/about.html">
      |""".stripMargin,
  )

  check(
    "cross-page",
    """
      |/docs/about.html
      |<a href="/index.html"></a>
      |
      |/index.html
      |<a href="/docs/about.html"></a>
      |""".stripMargin,
    """
      |/docs/about.html
      |<a href="../index.html"></a>
      |/index.html
      |<a href="docs/about.html"></a>
      |""".stripMargin,
  )

  check(
    "fragment is preserved",
    """
      |/index.html
      |<a href="#header"></a>
      |<a href="/docs/about.html#header"></a>
      |""".stripMargin,
    """
      |/index.html
      |<a href="index.html#header"></a>
      |<a href="docs/about.html#header"></a>
      |""".stripMargin,
  )

  check(
    "// is forced to https",
    """
      |/index.html
      |<a href="//cdnjs.cloudflare.com"></a>
      |""".stripMargin,
    """
      |/index.html
      |<a href="https://cdnjs.cloudflare.com"></a>
      |""".stripMargin,
  )

  check(
    "directory/ is expanded to directory/index.html",
    """
      |/docs/about.html
      |<a href="/"></a>
      |<a href="/docs"></a>
      |<a href="/docs/"></a>
      |<a href="/users/"></a>
      |/users/index.html
      |Users
      |""".stripMargin,
    """
      |/docs/about.html
      |<a href="../index.html"></a>
      |<a href="index.html"></a>
      |<a href="index.html"></a>
      |<a href="../users/index.html"></a>
      |/users/index.html
      |Users
      |""".stripMargin,
  )

}

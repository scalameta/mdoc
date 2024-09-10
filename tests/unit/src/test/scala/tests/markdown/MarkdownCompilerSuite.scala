package tests.markdown

import munit.FunSuite
import scala.meta._
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.MarkdownCompiler
import mdoc.internal.markdown.MarkdownBuilder
import mdoc.internal.markdown.Renderer
import mdoc.internal.markdown.ReplVariablePrinter
import mdoc.internal.cli.InputFile
import mdoc.internal.cli.Settings
import scala.meta.internal.io.PathIO
import mdoc.internal.cli.Context

class MarkdownCompilerSuite extends FunSuite {

  private val compiler = MarkdownBuilder.default()
  private val reporter = ConsoleReporter.default
  private val settings = Settings.default(PathIO.workingDirectory, "mdoc.properties")

  def checkIgnore(name: String, original: String, expected: String): Unit =
    test(name.ignore) {}

  def check(name: String, original: String, expected: String): Unit =
    check(name, original :: Nil, expected)

  def check(
      name: String,
      original: List[String],
      expected: String,
      compat: Map[Compat.ScalaVersion, String] = Map.empty
  ): Unit = {
    test(name) {
      val inputs = original.map(s => Input.String(s))
      val file = InputFile.fromRelativeFilename(name + ".md", settings)
      val context = Context(settings, reporter, compiler)
      val obtained = Renderer.render(
        file,
        inputs,
        compiler,
        settings,
        reporter,
        name + ".md",
        ReplVariablePrinter,
        context
      )
      assertNoDiff(
        obtained,
        Compat(expected, compat)
      )
    }
  }

  check(
    "two",
    List(
      """
        |val x = 1.to(10)
      """.stripMargin,
      """
        |val y = x.length
      """.stripMargin
    ),
    """
      |```scala
      |val x = 1.to(10)
      |// x: Range.Inclusive = Range(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
      |```
      |```scala
      |val y = x.length
      |// y: Int = 10
      |```
        """.stripMargin,
    compat = Map(
      Compat.Scala211 ->
        """
          |```scala
          |val x = 1.to(10)
          |// x: Range.Inclusive = Vector(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
          |```
          |```scala
          |val y = x.length
          |// y: Int = 10
          |```
          |""".stripMargin,
      Compat.Scala212 ->
        """
          |```scala
          |val x = 1.to(10)
          |// x: Range.Inclusive = Range.Inclusive(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
          |```
          |```scala
          |val y = x.length
          |// y: Int = 10
          |```
          |""".stripMargin,
      Compat.Scala3 ->
        """
          |```scala
          |val x = 1.to(10)
          |// x: Inclusive = Range(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
          |```
          |```scala
          |val y = x.length
          |// y: Int = 10
          |```
          |""".stripMargin
    )
  )

  check(
    "stdout",
    """
      |val x = {
      |  println(42)
      |  System.err.println("err")
      |  println(52)
      |  2
      |}
    """.stripMargin,
    """
      |```scala
      |val x = {
      |  println(42)
      |  System.err.println("err")
      |  println(52)
      |  2
      |}
      |// 42
      |// 52
      |// x: Int = 2
      |```
    """.stripMargin
  )

  check(
    "non-val",
    """println("hello world!")""",
    """
      |```scala
      |println("hello world!")
      |// hello world!
      |```
      |""".stripMargin
  )

  check(
    "comment1",
    """val x = 2 // comment""",
    """
      |```scala
      |val x = 2 // comment
      |// x: Int = 2
      |```
      |""".stripMargin
  )
}

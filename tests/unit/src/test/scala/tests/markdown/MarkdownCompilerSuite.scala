package tests.markdown

import scala.meta._
import scala.meta.testkit.DiffAssertions
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.MarkdownCompiler
import mdoc.internal.markdown.Renderer
import mdoc.internal.markdown.ReplVariablePrinter
import org.scalatest.funsuite.AnyFunSuite

class MarkdownCompilerSuite extends AnyFunSuite with DiffAssertions {

  private val compiler = MarkdownCompiler.default()
  private val reporter = ConsoleReporter.default

  def checkIgnore(name: String, original: String, expected: String): Unit =
    ignore(name) {}

  def check(name: String, original: String, expected: String): Unit =
    check(name, original :: Nil, expected)

  def check(
      name: String,
      original: List[String],
      expected: String,
      compat: Map[String, String] = Map.empty
  ): Unit = {
    test(name) {
      val inputs = original.map(s => Input.String(s))
      val obtained = Renderer.render(inputs, compiler, reporter, name + ".md", ReplVariablePrinter)
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
      "2.11" ->
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
      "2.12" ->
        """
          |```scala
          |val x = 1.to(10)
          |// x: Range.Inclusive = Range.Inclusive(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
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

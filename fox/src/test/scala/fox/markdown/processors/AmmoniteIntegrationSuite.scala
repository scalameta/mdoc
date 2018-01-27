package fox.markdown.processors

import fox.Markdown
import fox.markdown.repl.Evaluator

class AmmoniteIntegrationSuite extends BaseMarkdownSuite {
  check(
    "code",
    """
      |# Hey Scala!
      |
      |```scala fox
      |val xs = List(1, 2, 3)
      |val ys = xs.map(_ + 1)
      |```
      |
      |```scala fox
      |val zs = ys.map(_ * 2)
      |```
    """.stripMargin,
    """
      |# Hey Scala!
      |
      |```scala
      |@ val xs = List(1, 2, 3)
      |xs: List[Int] = List(1, 2, 3)
      |@ val ys = xs.map(_ + 1)
      |ys: List[Int] = List(2, 3, 4)
      |```
      |
      |```scala
      |@ val zs = ys.map(_ * 2)
      |zs: List[Int] = List(4, 6, 8)
      |```
    """.stripMargin
  )

  check(
    "passthrough",
    """
      |```scala fox:passthrough
      |println("# Header\n\nparagraph\n\n* bullet")
      |```
    """.stripMargin,
    """
      |# Header
      |
      |paragraph
      |
      |* bullet
    """.stripMargin
  )

  check(
    "fail",
    """
      |```scala fox:fail
      |val x: Int = "String"
      |```
    """.stripMargin,
    """
      |```scala
      |@ val x: Int = "String"
      |cmd0.sc:1: type mismatch;
      | found   : String("String")
      | required: Int
      |val x: Int = "String"
      |             ^
      |Compilation Failed
      |```
    """.stripMargin
  )

  checkError[Evaluator.CodeFenceFailure](
    "fail-error",
    """
      |```scala fox
      |foobar
      |```
    """.stripMargin,
    """/data/rw/code/scala/fox/dummy-test-fail-error:1:3: unexpected failure
      |>  cmd0.sc:1: not found: value foobar
      |>  val res0 = foobar
      |>             ^
      |>  Compilation Failed""".stripMargin
  )

  checkError[Evaluator.CodeFenceFailure](
    "fail-success",
    """
      |```scala fox:fail
      |1.to(2)
      |```
    """.stripMargin,
    """
      |/data/rw/code/scala/fox/dummy-test-fail-success:1:3: unexpected success of
      |```
      |1.to(2)
      |```""".stripMargin
  )

  checkError[Evaluator.CodeFenceFailure](
    "mixed-fail-success-error",
    """
      |```scala fox
      |foobar
      |```
      |
      |```scala fox:fail
      |1.to(2)
      |```
    """.stripMargin,
    """
      |/data/rw/code/scala/fox/dummy-test-mixed-fail-success-error:1:3: unexpected failure
      |>  cmd0.sc:1: not found: value foobar
      |>  val res0 = foobar
      |>             ^
      |>  Compilation Failed
      |
      |/data/rw/code/scala/fox/dummy-test-mixed-fail-success-error:5:7: unexpected success of
      |```
      |1.to(2)
      |```
      |""".stripMargin
  )
}

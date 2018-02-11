package vork.markdown.processors

import org.scalatest.Ignore

// Not implemented yet with new renderer
//@Ignore
class FailSuite extends BaseMarkdownSuite {

  check(
    "fail",
    """
      |```scala vork:fail
      |val x: Int = "String"
      |```
    """.stripMargin,
    """
      |```scala
      |@ val x: Int = "String"
      |type mismatch;
      | found   : String("String")
      | required: Int
      |val x: Int = "String"
      |             ^
      |```
    """.stripMargin
  )

  check(
    "triplequote",
    """
      |```scala vork:fail
      |val y: Int = '''Triplequote
      |newlines
      |'''
      |```
    """.replace("'''", "\"\"\"").stripMargin,
    """
      |```scala
      |@ val y: Int = '''Triplequote
      |newlines
      |'''
      |type mismatch;
      | found   : String("Triplequote\nnewlines\n")
      | required: Int
      |val y: Int = '''Triplequote
      |              ^
      |```
      |""".replace("'''", "\"\"\"").stripMargin
  )



//  check(
//    "fail-error",
//    """
//      |```scala vork
//      |foobar
//      |```
//    """.stripMargin,
//    """Vork found evaluation failures.
//      |
//      |<path>:1:3: unexpected failure
//      |>  cmd0.sc:1: not found: value foobar
//      |>  val res0 = foobar
//      |>             ^
//      |>  Compilation Failed
//      |""".stripMargin
//  )
//
//  checkError[Evaluator.CodeFenceFailure](
//    "fail-success",
//    """
//      |```scala vork:fail
//      |1.to(2)
//      |```
//    """.stripMargin,
//    """
//      |Vork found evaluation failures.
//      |
//      |<path>:1:3: unexpected success of
//      |```
//      |1.to(2)
//      |```
//      |""".stripMargin
//  )
//
//  checkError[Evaluator.CodeFenceFailure](
//    "mixed-fail-success-error",
//    """
//      |```scala vork
//      |foobar
//      |```
//      |
//      |```scala vork:fail
//      |1.to(2)
//      |```
//    """.stripMargin,
//    """
//      |Vork found evaluation failures.
//      |
//      |<path>:1:3: unexpected failure
//      |>  cmd0.sc:1: not found: value foobar
//      |>  val res0 = foobar
//      |>             ^
//      |>  Compilation Failed
//      |
//      |<path>:5:7: unexpected success of
//      |```
//      |1.to(2)
//      |```
//      |""".stripMargin
//  )
}

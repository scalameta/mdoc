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

  checkError(
    "fail-error",
    """
      |```scala vork
      |foobar
      |```
    """.stripMargin,
    """
      |error: fail-error.md not found: value foobar
      |foobar
      |^
      |""".stripMargin
  )

  checkError(
    "fail-success",
    """
      |```scala vork:fail
      |1.to(2)
      |```
    """.stripMargin,
    """
      |error: Expected compile error but the statement type-checked successfully to type scala.collection.immutable.Range.Inclusive:
      |1.to(2)
      |""".stripMargin
  )

  // Compile-error causes nothing to run
  checkError(
    "mixed-fail-success-error",
    """
      |```scala vork
      |val x = foobar
      |```
      |
      |```scala vork:fail
      |1.to(2)
      |```
    """.stripMargin,
    """
      |error: mixed-fail-success-error.md not found: value foobar
      |val x = foobar
      |        ^
      |""".stripMargin
  )
}

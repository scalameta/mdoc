package tests.markdown

import tests.markdown.StringSyntax._

class FailSuite extends BaseMarkdownSuite {

  check(
    "mismatch",
    """
      |```scala vork:fail
      |val x: Int = "String"
      |```
    """.stripMargin,
    """
      |```scala
      |val x: Int = "String"
      |// type mismatch;
      |//  found   : String("String")
      |//  required: Int
      |// val x: Int = "String"
      |//              ^
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
    """.stripMargin.triplequoted,
    """
      |```scala
      |val y: Int = '''Triplequote
      |newlines
      |'''
      |// type mismatch;
      |//  found   : String("Triplequote\nnewlines\n")
      |//  required: Int
      |// val y: Int = '''Triplequote
      |//              ^
      |```
      |""".stripMargin.triplequoted
  )

  checkError(
    "fail-error",
    """
      |```scala vork
      |foobar
      |```
    """.stripMargin,
    """
      |error: fail-error.md:3:1: error: not found: value foobar
      |foobar
      |^^^^^^
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
      |error: fail-success.md:3:1: error: Expected compile error but statement type-checked successfully
      |1.to(2)
      |^^^^^^^
      |""".stripMargin
  )

  // Compile-error causes nothing to run
  checkError(
    "mixed-error",
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
      |error: mixed-error.md:3:9: error: not found: value foobar
      |val x = foobar
      |        ^^^^^^
      |""".stripMargin
  )

}

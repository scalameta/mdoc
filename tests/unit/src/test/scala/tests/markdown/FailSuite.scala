package tests.markdown

import tests.markdown.StringSyntax._

class FailSuite extends BaseMarkdownSuite {

  check(
    "mismatch",
    """
      |```scala mdoc:fail
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
      |```scala mdoc:fail
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
      |```scala mdoc
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
      |```scala mdoc:fail
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
      |```scala mdoc
      |val x = foobar
      |```
      |
      |```scala mdoc:fail
      |1.to(2)
      |```
    """.stripMargin,
    """
      |error: mixed-error.md:3:9: error: not found: value foobar
      |val x = foobar
      |        ^^^^^^
      |""".stripMargin
  )

  check(
    "order",
    """
      |```scala mdoc
      |println(42)
      |```
      |```scala mdoc:fail
      |val x: Int = "String"
      |```
    """.stripMargin,
    """
      |```scala
      |println(42)
      |// 42
      |```
      |
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
    "fs2",
    """
      |```scala mdoc:fail
      |fs2.Stream.eval(println("Do not ever do this"))
      |```
    """.stripMargin,
    // NOTE(olafur) https://github.com/olafurpg/mdoc/issues/95#issuecomment-426993507
    // The error message below does not match the compiler error reported in the REPL.
    // We should reconsider the architecture for the `fail` modifier.
    """|```scala
       |fs2.Stream.eval(println("Do not ever do this"))
       |// type mismatch;
       |//  found   : Unit
       |//  required: ?F[?O]
       |// Note that implicit conversions are not applicable because they are ambiguous:
       |//  both method ArrowAssoc in object Predef of type [A](self: A)ArrowAssoc[A]
       |//  and method Ensuring in object Predef of type [A](self: A)Ensuring[A]
       |//  are possible conversion functions from Unit to ?F[?O]
       |```
    """.stripMargin
  )

}

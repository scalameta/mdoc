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
    """|```scala
       |val x: Int = "String"
       |// error: type mismatch;
       |//  found   : String("String")
       |//  required: Int
       |// val x: Int = "String"
       |//              ^^^^^^^^
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
      |// error: type mismatch;
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
      |error: fail-success.md:3:1: error: Expected compile error but statement typechecked successfully
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
    """|```scala
       |println(42)
       |// 42
       |```
       |
       |```scala
       |val x: Int = "String"
       |// error: type mismatch;
       |//  found   : String("String")
       |//  required: Int
       |// val x: Int = "String"
       |//              ^^^^^^^^
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
    // See https://github.com/scalameta/mdoc/issues/95#issuecomment-426993507
    """|```scala
       |fs2.Stream.eval(println("Do not ever do this"))
       |// error: no type parameters for method eval: (fo: F[O])fs2.Stream[F,O] exist so that it can be applied to arguments (Unit)
       |//  --- because ---
       |// argument expression's type is not compatible with formal parameter type;
       |//  found   : Unit
       |//  required: ?F[?O]
       |// fs2.Stream.eval(println("Do not ever do this"))
       |// ^^^^^^^^^^^^^^^
       |// error: type mismatch;
       |//  found   : Unit
       |//  required: F[O]
       |// fs2.Stream.eval(println("Do not ever do this"))
       |//                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
       |```
    """.stripMargin
  )

  check(
    "double",
    """
      |```scala mdoc:fail
      |println(a)
      |println(b)
      |```
    """.stripMargin,
    """|```scala
       |println(a)
       |println(b)
       |// error: not found: value a
       |// println(a)
       |//         ^
       |// error: not found: value b
       |// println(b)
       |//         ^
       |```
    """.stripMargin
  )

  check(
    "edit",
    """
      |```scala mdoc:fail
      |val x = 1
      |println(a)
      |println(b)
      |```
      |
      |```scala mdoc:fail
      |val x = 1
      |println(c)
      |println(d)
      |```
    """.stripMargin,
    """|```scala
       |val x = 1
       |println(a)
       |println(b)
       |// error: not found: value a
       |// error: not found: value b
       |```
       |
       |```scala
       |val x = 1
       |println(c)
       |println(d)
       |// error: not found: value c
       |// println(c)
       |//         ^
       |// error: not found: value d
       |// println(d)
       |//         ^
       |```
    """.stripMargin
  )

}

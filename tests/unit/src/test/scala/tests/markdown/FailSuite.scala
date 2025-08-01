package tests.markdown

import tests.markdown.StringSyntax._

class FailSuite extends BaseMarkdownSuite {

  override def postProcessExpected: Map[Compat.ScalaVersion, String => String] =
    Map(
      Compat.Scala213 -> { old =>
        old.replace("(fo: F[O])fs2.Stream[F,O]", "(fo: F[O]): fs2.Stream[F,O]")
      }
    )

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
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |val x: Int = "String"
           |// error:
           |// Found:    ("String" : String)
           |// Required: Int
           |// val x: Int = "String"
           |//              ^^^^^^^^
           |```
    """.stripMargin
    )
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
      |""".stripMargin.triplequoted,
    compat = Map(
      Compat.Scala3 ->
        """
          |```scala
          |val y: Int = '''Triplequote
          |newlines
          |'''
          |// error:
          |// Found:    ("Triplequote\nnewlines\n" : String)
          |// Required: Int
          |// val y: Int = '''Triplequote
          |//              ^
          |```
          |""".stripMargin.triplequoted
    )
  )

  checkError(
    "fail-error",
    """
      |```scala mdoc
      |foobar
      |```
    """.stripMargin,
    """
      |error: fail-error.md:3:1: not found: value foobar
      |foobar
      |^^^^^^
      |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """
          |error: fail-error.md:3:1:
          |Not found: foobar
          |foobar
          |^^^^^^
        """.stripMargin
    )
  )

  checkError(
    "fail-success",
    """
      |```scala mdoc:fail
      |1.to(2)
      |```
    """.stripMargin,
    """|error: fail-success.md:3:1: Expected compile errors but program compiled successfully without errors
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
      |error: mixed-error.md:3:9: not found: value foobar
      |val x = foobar
      |        ^^^^^^
      |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """
          |error: mixed-error.md:3:9:
          |Not found: foobar
          |val x = foobar
          |        ^^^^^^
        """.stripMargin
    )
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
       |```scala
       |val x: Int = "String"
       |// error: type mismatch;
       |//  found   : String("String")
       |//  required: Int
       |// val x: Int = "String"
       |//              ^^^^^^^^
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |println(42)
           |// 42
           |```
           |```scala
           |val x: Int = "String"
           |// error:
           |// Found:    ("String" : String)
           |// Required: Int
           |// val x: Int = "String"
           |//              ^^^^^^^^
           |```
    """.stripMargin
    )
  )

  check(
    "fs2",
    """
      |```scala mdoc:fail
      |fs2.Stream.eval(println("Do not ever do this"))
      |```
    """.stripMargin,
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
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |fs2.Stream.eval(println("Do not ever do this"))
           |// error:
           |// Found:    Unit
           |// Required: ([_$$17] =>> Any)[Any]
           |// Note that implicit conversions were not tried because the result of an implicit conversion
           |// must be more specific than ([_$$17] =>> Any)[Any]
           |// fs2.Stream.eval(println("Do not ever do this"))
           |//                 ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
           |```
    """.stripMargin
    )
  )

  check(
    "double",
    """
      |```scala mdoc:fail
      |println(notfound)
      |println(b)
      |```
    """.stripMargin,
    """|```scala
       |println(notfound)
       |println(b)
       |// error: not found: value notfound
       |// println(notfound)
       |//         ^^^^^^^^
       |// error: not found: value b
       |// println(b)
       |//         ^
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |println(notfound)
           |println(b)
           |// error:
           |// Not found: notfound
           |// println(notfound)
           |//         ^^^^^^^^
           |// error:
           |// Not found: b
           |// println(b)
           |//         ^
           |```
    """.stripMargin
    )
  )

  check(
    "edit",
    """
      |```scala mdoc:fail
      |val x = 1
      |println(notfound)
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
       |println(notfound)
       |println(b)
       |// error: not found: value notfound
       |// println(notfound)
       |//         ^^^^^^^^
       |// error: not found: value b
       |// println(b)
       |//         ^
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
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |val x = 1
           |println(notfound)
           |println(b)
           |// error:
           |// Not found: notfound
           |// println(notfound)
           |//         ^^^^^^^^
           |// error:
           |// Not found: b
           |// println(b)
           |//         ^
           |```
           |
           |```scala
           |val x = 1
           |println(c)
           |println(d)
           |// error:
           |// Not found: c
           |// println(c)
           |//         ^
           |// error:
           |// Not found: d
           |// println(d)
           |//         ^
           |```
    """.stripMargin
    )
  )

  check(
    "value-class",
    """
      |```scala mdoc:reset-object
      |final case class FloatValue(val value: Float) extends AnyVal
      |```
      |
      |```scala mdoc:fail
      |"abc": Int
      |```
    """.stripMargin,
    """|
       |```scala
       |final case class FloatValue(val value: Float) extends AnyVal
       |```
       |
       |```scala
       |"abc": Int
       |// error: type mismatch;
       |//  found   : String("abc")
       |//  required: Int
       |// "abc": Int
       |// ^^^^^
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|
           |```scala
           |final case class FloatValue(val value: Float) extends AnyVal
           |```
           |
           |```scala
           |"abc": Int
           |// error:
           |// Found:    ("abc" : String)
           |// Required: Int
           |// "abc": Int
           |// ^^^^^
           |```
    """.stripMargin
    )
  )

  check(
    "two-errors",
    """
      |```scala mdoc:fail
      |val one = "foo" * "goo"
      |```
      |
      |```scala mdoc:fail
      |val two = "moo" * "loo"
      |```
    """.stripMargin,
    """|```scala
       |val one = "foo" * "goo"
       |// error: type mismatch;
       |//  found   : String("goo")
       |//  required: Int
       |// val one = "foo" * "goo"
       |//                   ^^^^^
       |```
       |
       |```scala
       |val two = "moo" * "loo"
       |// error: type mismatch;
       |//  found   : String("loo")
       |//  required: Int
       |// val two = "moo" * "loo"
       |//                   ^^^^^
       |```
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |val one = "foo" * "goo"
           |// error:
           |// Found:    ("goo" : String)
           |// Required: Int
           |// val one = "foo" * "goo"
           |//                   ^^^^^
           |```
           |
           |```scala
           |val two = "moo" * "loo"
           |// error:
           |// Found:    ("loo" : String)
           |// Required: Int
           |// val two = "moo" * "loo"
           |//                   ^^^^^
           |```
           |""".stripMargin
    )
  )

  check(
    "error-max-height",
    """
      |```scala mdoc:fail:height=2
      |val x = 1
      |println(notfound)
      |```
    """.stripMargin,
    """|```scala
       |val x = 1
       |println(notfound)
       |// error: not found: value notfound
       |// println(notfound)
       |// ...
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |val x = 1
           |println(notfound)
           |// error:
           |// Not found: notfound
           |// ...
           |```
    """.stripMargin
    )
  )

  check(
    "error-max-width",
    """
      |```scala mdoc:fail:width=30
      |fs2.Stream.eval(println("Do not ever do this"))
      |```
    """.stripMargin,
    """|```scala
       |fs2.Stream.eval(println("Do not ever do this"))
       |// error: no type parameters for ...
       |//  --- because ---
       |// argument expression's type is ...
       |//  found   : Unit
       |//  required: ?F[?O]
       |// fs2.Stream.eval(println("Do no...
       |// ^^^^^^^^^^^^^^^
       |// error: type mismatch;
       |//  found   : Unit
       |//  required: F[O]
       |// fs2.Stream.eval(println("Do no...
       |//                 ^^^^^^^^^^^^^^...
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |fs2.Stream.eval(println("Do not ever do this"))
           |// error:
           |// Found:    Unit
           |// Required: ([_$$17] =>> Any)[An...
           |// Note that implicit conversions...
           |// must be more specific than ([_...
           |// fs2.Stream.eval(println("Do no...
           |//                 ^^^^^^^^^^^^^^...
           |```
    """.stripMargin
    )
  )

  check(
    "error-max-width-and-height",
    """
      |```scala mdoc:fail:width=30:height=5
      |fs2.Stream.eval(println("Do not ever do this"))
      |```
    """.stripMargin,
    """|```scala
       |fs2.Stream.eval(println("Do not ever do this"))
       |// error: no type parameters for ...
       |//  --- because ---
       |// argument expression's type is ...
       |//  found   : Unit
       |//  required: ?F[?O]
       |// ...
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |fs2.Stream.eval(println("Do not ever do this"))
           |// error:
           |// Found:    Unit
           |// Required: ([_$$17] =>> Any)[An...
           |// Note that implicit conversions...
           |// must be more specific than ([_...
           |// ...
           |```
    """.stripMargin
    )
  )

}

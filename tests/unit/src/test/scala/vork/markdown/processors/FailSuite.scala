package vork.markdown.processors

import StringSyntax._

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
    """.stripMargin.triplequoted,
    """
      |```scala
      |@ val y: Int = '''Triplequote
      |newlines
      |'''
      |type mismatch;
      | found   : String("Triplequote\nnewlines\n")
      | required: Int
      |val y: Int = '''Triplequote
      |             ^
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

  checkError(
    "crash",
    """
      |```scala vork
      |val x = 1
      |```
      |```scala vork
      |val y = 2
      |def crash() = ???
      |def z: Int = crash()
      |def safeMethod = 1 + 2
      |x + y + z
      |```
    """.stripMargin,
    """
      |error: crash.md:10:1: error: an implementation is missing
      |x + y + z
      |^^^^^^^^^
      |scala.NotImplementedError: an implementation is missing
      |	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:284)
      |	at repl.Session.crash$1(crash.md:16)
      |	at repl.Session.z$1(crash.md:18)
      |	at repl.Session.$anonfun$app$9(crash.md:23)
      |	at scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.java:12)
      |""".stripMargin
  )
}

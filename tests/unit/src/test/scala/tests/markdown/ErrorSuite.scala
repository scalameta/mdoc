package tests.markdown

class ErrorSuite extends BaseMarkdownSuite {

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
      |	at repl.Session.crash$1(crash.md:14)
      |	at repl.Session.z$1(crash.md:16)
      |	at repl.Session.$anonfun$app$9(crash.md:21)
      |	at scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.java:12)
      |""".stripMargin
  )

  checkError(
    "invalid-mod",
    """
      |```scala vork:foobaz
      |val x: Int = "String"
      |```
    """.stripMargin,
    """
      |error: invalid-mod.md:2:15: error: Invalid mode 'foobaz'. Expected one of: default, passthrough, fail
      |```scala vork:foobaz
      |              ^^^^^^
    """.stripMargin
  )

  checkError(
    "silent",
    """
      |```scala vork:passthrough
      |import scala.util._
      |```
      |
      |```scala vork:fail
      |List(1)
      |```
    """.stripMargin,
    """
      |error: silent.md:7:1: error: Expected compile error but statement type-checked successfully
      |List(1)
      |^^^^^^^
    """.stripMargin
  )
  checkError(
    "parse-error",
    """
      |```scala vork
      |val x =
      |```
    """.stripMargin,
    """
      |error: parse-error.md:3:8: error: illegal start of simple expression
      |val x =
      |       ^
    """.stripMargin
  )
}

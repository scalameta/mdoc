package tests.markdown

class ErrorSuite extends BaseMarkdownSuite {

  checkError(
    "crash",
    """
      |```scala mdoc
      |val x = 1
      |```
      |```scala mdoc
      |val y = 2
      |def crash() = ???
      |def z: Int = crash()
      |def safeMethod = 1 + 2
      |x + y + z
      |```
    """.stripMargin,
    """|error: crash.md:10:1: an implementation is missing
       |x + y + z
       |^^^^^^^^^
       |scala.NotImplementedError: an implementation is missing
       |	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:288)
       |	at repl.Session$App$.crash(crash.md:17)
       |	at repl.Session$App$.z(crash.md:20)
       |	at repl.Session$App$.<init>(crash.md:26)
       |	at repl.Session$App$.<clinit>(crash.md)
       |	at repl.Session$.app(crash.md:3)
       |""".stripMargin
  )

  checkError(
    "invalid-mod",
    """
      |```scala mdoc:foobaz
      |val x: Int = "String"
      |```
    """.stripMargin,
    """
      |error: invalid-mod.md:2:15: Invalid mode 'foobaz'
      |```scala mdoc:foobaz
      |              ^^^^^^
    """.stripMargin
  )

  checkError(
    "silent",
    """
      |```scala mdoc:passthrough
      |import scala.util._
      |```
      |
      |```scala mdoc:fail
      |List(1)
      |```
    """.stripMargin,
    """
      |error: silent.md:7:1: Expected compile error but statement typechecked successfully
      |List(1)
      |^^^^^^^
    """.stripMargin
  )
  checkError(
    "parse-error",
    """
      |```scala mdoc
      |val x =
      |```
    """.stripMargin,
    """
      |error: parse-error.md:3:8: illegal start of simple expression
      |val x =
      |       ^
    """.stripMargin
  )
  checkError(
    "not-member",
    """
      |```scala mdoc
      |List(1).len
      |```
    """.stripMargin,
    """|error: not-member.md:3:1: value len is not a member of List[Int]
       |List(1).len
       |^^^^^^^^^^^
    """.stripMargin
  )

  checkError(
    "already-defined",
    """
      |```scala mdoc
      |val x = 1
      |val x = 2
      |```
    """.stripMargin,
    """|error: already-defined.md:4:5: x is already defined as value x
       |val x = 2
       |    ^
    """.stripMargin
  )

  checkError(
    "yrangepos",
    """
      |```scala mdoc
      |List[Int]("".length.toString)
      |```
    """.stripMargin,
    """|error: yrangepos.md:3:11: type mismatch;
       | found   : String
       | required: Int
       |List[Int]("".length.toString)
       |          ^^^^^^^^^^^^^^^^^^
    """.stripMargin
  )

  checkError(
    "multimods-typo",
    """
      |```scala mdoc:reset:silen
      |List[Int]("".length.toString)
      |```
    """.stripMargin,
    """|error: multimods-typo.md:2:15: Invalid mode 'reset:silen'
       |```scala mdoc:reset:silen
       |              ^^^^^^^^^^^
    """.stripMargin
  )
}

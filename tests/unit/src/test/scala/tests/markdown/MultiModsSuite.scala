package tests.markdown

class MultiModsSuite extends BaseMarkdownSuite {

  check(
    "reset:fail",
    """
      |```scala mdoc
      |val x = 1
      |```
      |
      |```scala mdoc:reset:fail
      |println(x)
      |```
      |""".stripMargin,
    """|
       |```scala
       |val x = 1
       |// x: Int = 1
       |```
       |
       |```scala
       |println(x)
       |// error: not found: value x
       |// println(x)
       |//         ^
       |```
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|
           |```scala
           |val x = 1
           |// x: Int = 1
           |```
           |
           |```scala
           |println(x)
           |// error:
           |// Not found: x
           |// println(x)
           |//         ^
           |```
           |""".stripMargin
    )
  )

  check(
    "reset:silent",
    """
      |```scala mdoc
      |val x = 1
      |```
      |
      |```scala mdoc:reset:silent
      |val x = 1
      |```
    """.stripMargin,
    """|```scala
       |val x = 1
       |// x: Int = 1
       |```
       |
       |```scala
       |val x = 1
       |```
    """.stripMargin
  )

  check(
    "reset:invisible",
    """
      |```scala mdoc
      |val x = 1
      |```
      |
      |```scala mdoc:reset:invisible
      |val x = 2
      |```
      |
      |```scala mdoc
      |println(x)
      |```
    """.stripMargin,
    """|```scala
       |val x = 1
       |// x: Int = 1
       |```
       |
       |
       |```scala
       |println(x)
       |// 2
       |```
    """.stripMargin
  )

  checkError(
    "silent:invisible",
    """
      |```scala mdoc:reset:silent:invisible
      |val x = 2
      |```
    """.stripMargin,
    """|error: silent:invisible.md:2:15: invalid combination of modifiers 'silent' and 'invisible'
       |```scala mdoc:reset:silent:invisible
       |              ^^^^^^^^^^^^^^^^^^^^^^
    """.stripMargin
  )

  checkError(
    "fail:crash",
    """
      |```scala mdoc:reset:fail:crash
      |val x = 2
      |```
    """.stripMargin,
    """|error: fail:crash.md:2:15: invalid combination of modifiers 'crash' and 'fail'
       |```scala mdoc:reset:fail:crash
       |              ^^^^^^^^^^^^^^^^
    """.stripMargin
  )

  checkError(
    "compile-only:passthrough",
    """
      |```scala mdoc:compile-only:passthrough
      |val x = 2
      |```
    """.stripMargin,
    """|error: compile-only:passthrough.md:2:15: compile-only cannot be used in combination with passthrough
       |```scala mdoc:compile-only:passthrough
       |              ^^^^^^^^^^^^^^^^^^^^^^^^
    """.stripMargin
  )

  check(
    "nest:multi",
    """
      |```scala mdoc:nest:passthrough:to-string
      |println("* 42")
      |```
    """.stripMargin,
    """|* 42
    """.stripMargin
  )

  checkError(
    "nest:reset",
    """
      |```scala mdoc
      |val x = "* 42"
      |```
      |```scala mdoc:nest:reset
      |val x = "* 43"
      |```
    """.stripMargin,
    """|error: nest:reset.md:5:15: the modifier 'nest' is redundant when used in combination with 'reset'. To fix this error, remove 'nest'
       |```scala mdoc:nest:reset
       |              ^^^^^^^^^^
       |""".stripMargin
  )

  checkError(
    "compile-only:multiple",
    """
      |```scala mdoc:compile-only:to-string:silent
      |val x = 2
      |```
    """.stripMargin,
    """|error: compile-only:multiple.md:2:15: compile-only cannot be used in combination with to-string, silent
       |```scala mdoc:compile-only:to-string:silent
       |              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    """.stripMargin
  )
}

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
    """.stripMargin,
    """|```scala
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
    """.stripMargin
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
    """|error: silent:invisible.md:2:15: error: invalid combination of modifiers 'silent' and 'invisible' are
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
    """|error: fail:crash.md:2:15: error: invalid combination of modifiers 'crash' and 'fail' are
       |```scala mdoc:reset:fail:crash
       |              ^^^^^^^^^^^^^^^^
    """.stripMargin
  )

}

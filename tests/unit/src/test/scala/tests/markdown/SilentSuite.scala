package tests.markdown

class SilentSuite extends BaseMarkdownSuite {

  check(
    "basic",
    """
      |```scala mdoc:silent
      |val x = 4
      |```
      |
      |```scala mdoc
      |println(x)
      |```
    """.stripMargin,
    """|```scala
       |val x = 4
       |```
       |
       |```scala
       |println(x)
       |// 4
       |```
    """.stripMargin
  )

  checkError(
    "error".tag(SkipScala3),
    """
      |```scala mdoc:silent
      |val x: String = 4
      |```
    """.stripMargin,
    """|error: error.md:3:17: type mismatch;
       | found   : Int(4)
       | required: String
       |val x: String = 4
       |                ^
       |""".stripMargin
  )

  checkError(
    "error-scala3".tag(OnlyScala3),
    """
      |```scala mdoc:silent
      |val x: String = 4
      |```
    """.stripMargin,
    """|error: error-scala3.md:3:17:
       |Found:    (4 : Int)
       |Required: String
       |val x: String = 4
       |                ^
       |""".stripMargin
  )
}

package tests.markdown

class WarnSuite extends BaseMarkdownSuite {

  check(
    "warn",
    """
      |```scala mdoc:warn
      |List(1) match { case Nil => }
      |```
    """.stripMargin,
    """|```scala
       |List(1) match { case Nil => }
       |// warning: match may not be exhaustive.
       |// It would fail on the following input: List(_)
       |// List(1) match { case Nil => }
       |// ^^^^^^^
       |```
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 -> 
        """
          |
          | warn:
          | match may not be exhaustive.
          | 
          | It would fail on pattern case: List(_, _*)
          | 
        """.stripMargin
    )
  )

  checkError(
    "error",
    """
      |```scala mdoc:warn
      |val x: Int = ""
      |```
    """.stripMargin,
    """|error: error.md:3:1: Expected compile warnings but program failed to compile
       |val x: Int = ""
       |^^^^^^^^^^^^^^^
       |""".stripMargin
  )

  checkError(
    "success",
    """
      |```scala mdoc:warn
      |val x: Int = 42
      |```
    """.stripMargin,
    """|error: success.md:3:1: Expected compile warnings but program compiled successfully without warnings
       |val x: Int = 42
       |^^^^^^^^^^^^^^^
       |""".stripMargin
  )
}

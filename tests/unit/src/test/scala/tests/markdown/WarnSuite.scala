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

  check(
    "warn-max-height",
    """
      |```scala mdoc:warn:height=3
      |List(1) match { case Nil => }
      |```
    """.stripMargin,
    """|```scala
       |List(1) match { case Nil => }
       |// warning: match may not be exhaustive.
       |// It would fail on the following input: List(_)
       |// List(1) match { case Nil => }
       |// ...
       |```
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """
          |
          | warn:
          | match may not be exhaustive.
          | ...
        """.stripMargin
    )
  )

  check(
    "warn-max-width",
    """
      |```scala mdoc:warn:width=20
      |List(1) match { case Nil => }
      |```
    """.stripMargin,
    """|```scala
       |List(1) match { case Nil => }
       |// warning: match may n...
       |// It would fail on the...
       |// List(1) match { case...
       |// ^^^^^^^
       |```
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """
          |
          | warn:
          | match may not be exh...
          | 
          | It would fail on pat...
          | 
        """.stripMargin
    )
  )

  check(
    "warn-max-width-and-height",
    """
      |```scala mdoc:warn:width=20:height=2
      |List(1) match { case Nil => }
      |```
    """.stripMargin,
    """|```scala
       |List(1) match { case Nil => }
       |// warning: match may n...
       |// It would fail on the...
       |// ...
       |```
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """
          |
          | warn:
          | ...
        """.stripMargin
    )
  )
}

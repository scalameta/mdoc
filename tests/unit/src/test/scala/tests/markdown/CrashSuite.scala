package tests.markdown

class CrashSuite extends BaseMarkdownSuite {
  check(
    "basic",
    """
      |```scala mdoc:crash
      |val x = 1
      |???
      |```
    """.stripMargin,
    """
      |```scala
      |val x = 1
      |???
      |// scala.NotImplementedError: an implementation is missing
      |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:288)
      |// 	at repl.Session.$anonfun$app$2(basic.md:13)
      |```
    """.stripMargin
  )

  checkError(
    "definition",
    """
      |```scala mdoc:crash
      |case class User(name: String)
      |```
    """.stripMargin,
    """
      |error: definition.md:3:1: error: Expected runtime exception but program completed successfully
      |case class User(name: String)
      |^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    """.stripMargin
  )

  checkError(
    "false-positive",
    """
      |```scala mdoc:crash
      |"ab".length
      |```
  """.stripMargin,
    """
      |error: false-positive.md:3:1: error: Expected runtime exception but program completed successfully
      |"ab".length
      |^^^^^^^^^^^
    """.stripMargin
  )

}

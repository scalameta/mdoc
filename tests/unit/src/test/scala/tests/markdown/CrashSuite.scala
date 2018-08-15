package tests.markdown

class CrashSuite extends BaseMarkdownSuite {
  check(
    "basic",
    """
      |```scala vork:crash
      |val x = 1
      |???
      |```
    """.stripMargin,
    """
      |```scala
      |val x = 1
      |???
      |scala.NotImplementedError: an implementation is missing
      |	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:284)
      |	at repl.Session.$anonfun$app$4(basic.md:7)
      |	at scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.java:12)
      |```
    """.stripMargin
  )

  checkError(
    "definition",
    """
      |```scala vork:crash
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
      |```scala vork:crash
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

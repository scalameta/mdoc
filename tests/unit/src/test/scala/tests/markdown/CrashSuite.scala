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
    """|```scala
       |val x = 1
       |???
       |// scala.NotImplementedError: an implementation is missing
       |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:288)
       |// 	at repl.Session$App$$anonfun$2.apply(basic.md:14)
       |// 	at repl.Session$App$$anonfun$2.apply(basic.md:14)
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

  check(
    "comments",
    """
      |```scala mdoc:crash
      |1 match {
      |  case 2 => // boom!
      |}
      |```
    """.stripMargin,
    """|```scala
       |1 match {
       |  case 2 => // boom!
       |}
       |// scala.MatchError: 1 (of class java.lang.Integer)
       |// 	at repl.Session$App$$anonfun$1.apply(comments.md:9)
       |```
    """.stripMargin
  )

  check(
    "path/to/relative",
    """
      |```scala mdoc:crash
      |???
      |```
    """.stripMargin,
    """|```scala
       |???
       |// scala.NotImplementedError: an implementation is missing
       |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:288)
       |// 	at repl.Session$App$$anonfun$1.apply(relative.md:9)
       |// 	at repl.Session$App$$anonfun$1.apply(relative.md:9)
       |```
    """.stripMargin
  )

}

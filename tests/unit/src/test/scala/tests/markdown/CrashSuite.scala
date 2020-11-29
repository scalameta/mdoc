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
       |// 	at repl.MdocSession$App$$anonfun$3.apply(basic.md:14)
       |// 	at repl.MdocSession$App$$anonfun$3.apply(basic.md:14)
       |```
    """.stripMargin,
    compat = Map(
      "2.13" ->
        """|```scala
           |val x = 1
           |???
           |// scala.NotImplementedError: an implementation is missing
           |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:347)
           |// 	at repl.MdocSession$App$$anonfun$3.apply(basic.md:14)
           |// 	at repl.MdocSession$App$$anonfun$3.apply(basic.md:14)
           |```
    """.stripMargin
    )
  )

  checkError(
    "definition",
    """
      |```scala mdoc:crash
      |case class User(name: String)
      |```
    """.stripMargin,
    """
      |error: definition.md:3:1: Expected runtime exception but program completed successfully
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
      |error: false-positive.md:3:1: Expected runtime exception but program completed successfully
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
       |// 	at repl.MdocSession$App$$anonfun$1.apply(comments.md:9)
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
       |// 	at repl.MdocSession$App$$anonfun$1.apply(relative.md:9)
       |// 	at repl.MdocSession$App$$anonfun$1.apply(relative.md:9)
       |```
    """.stripMargin,
    compat = Map(
      "2.13" ->
        """|```scala
           |???
           |// scala.NotImplementedError: an implementation is missing
           |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:347)
           |// 	at repl.MdocSession$App$$anonfun$1.apply(relative.md:9)
           |// 	at repl.MdocSession$App$$anonfun$1.apply(relative.md:9)
           |```
    """.stripMargin
    )
  )

  check(
    "fatal",
    """
      |```scala mdoc:crash
      |throw new StackOverflowError()
      |```
    """.stripMargin,
    """|```scala
       |throw new StackOverflowError()
       |// java.lang.StackOverflowError
       |// 	at repl.MdocSession$App$$anonfun$1.apply(fatal.md:9)
       |// 	at repl.MdocSession$App$$anonfun$1.apply(fatal.md:9)
       |```
       |""".stripMargin
  )

  check(
    "fatal2",
    """
      |```scala mdoc:crash
      |throw new NoClassDefFoundError()
      |```
    """.stripMargin,
    """|```scala
       |throw new NoClassDefFoundError()
       |// java.lang.NoClassDefFoundError
       |// 	at repl.MdocSession$App$$anonfun$1.apply(fatal2.md:9)
       |// 	at repl.MdocSession$App$$anonfun$1.apply(fatal2.md:9)
       |```
       |""".stripMargin
  )

  check(
    "fatal3",
    """
      |```scala mdoc:crash
      |throw new NoSuchMethodError()
      |```
    """.stripMargin,
    """|```scala
       |throw new NoSuchMethodError()
       |// java.lang.NoSuchMethodError
       |// 	at repl.MdocSession$App$$anonfun$1.apply(fatal3.md:9)
       |// 	at repl.MdocSession$App$$anonfun$1.apply(fatal3.md:9)
       |```
       |""".stripMargin
  )

  check(
    "fatal4",
    """
      |```scala mdoc:crash
      |throw new IncompatibleClassChangeError()
      |```
    """.stripMargin,
    """|```scala
       |throw new IncompatibleClassChangeError()
       |// java.lang.IncompatibleClassChangeError
       |// 	at repl.MdocSession$App$$anonfun$1.apply(fatal4.md:9)
       |// 	at repl.MdocSession$App$$anonfun$1.apply(fatal4.md:9)
       |```
       |""".stripMargin
  )

}

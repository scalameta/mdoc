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
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(basic.md:10)
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(basic.md:8)
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala213 ->
        """|```scala
           |val x = 1
           |???
           |// scala.NotImplementedError: an implementation is missing
           |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:347)
           |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(basic.md:10)
           |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(basic.md:8)
           |```
    """.stripMargin,
      Compat.Scala3 ->
        """|```scala
           |val x = 1
           |???
           |// scala.NotImplementedError: an implementation is missing
           |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:347)
           |// 	at repl.MdocSession$MdocApp.$init$$anonfun$3(basic.md:14)
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
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(comments.md:9)
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |1 match {
           |  case 2 => // boom!
           |}
           |// scala.MatchError: 1 (of class java.lang.Integer)
           |// 	at repl.MdocSession$MdocApp.$init$$anonfun$1(comments.md:9)
           |//  at scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
           |```
    """.stripMargin
    )
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
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(relative.md:9)
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(relative.md:9)
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala213 ->
        """|```scala
           |???
           |// scala.NotImplementedError: an implementation is missing
           |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:347)
           |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(relative.md:9)
           |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(relative.md:9)
           |```
    """.stripMargin,
      Compat.Scala3 ->
        """|```scala
           |???
           |// scala.NotImplementedError: an implementation is missing
           |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:347)
           |// 	at repl.MdocSession$MdocApp.$init$$anonfun$1(relative.md:9)
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
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(fatal.md:9)
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(fatal.md:9)
       |```
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |throw new StackOverflowError()
           |// java.lang.StackOverflowError
           |// 	at repl.MdocSession$MdocApp.$init$$$anonfun$1(fatal.md:9)
           |```
           |""".stripMargin
    )
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
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(fatal2.md:9)
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(fatal2.md:9)
       |```
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |throw new NoClassDefFoundError()
           |// java.lang.NoClassDefFoundError
           |// 	at repl.MdocSession$MdocApp.$init$$$anonfun$1(fatal2.md:9)
           |```
           |""".stripMargin
    )
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
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(fatal3.md:9)
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(fatal3.md:9)
       |```
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |throw new NoSuchMethodError()
           |// java.lang.NoSuchMethodError
           |// 	at repl.MdocSession$MdocApp.$init$$$anonfun$1(fatal3.md:9)
           |```
           |""".stripMargin
    )
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
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(fatal4.md:9)
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(fatal4.md:9)
       |```
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|```scala
           |throw new IncompatibleClassChangeError()
           |// java.lang.IncompatibleClassChangeError
           |// 	at repl.MdocSession$MdocApp.$init$$$anonfun$1(fatal4.md:9)
           |```
           |""".stripMargin
    )
  )

  check(
    "significant-indentation".tag(OnlyScala3),
    """
      |```scala mdoc:nest
      |println("what!")
      |```
      |
      |```scala mdoc:crash
      |def hello(x: Int) =
      |  if x != 0 then
      |    println(x)
      |    x / 0
      |hello(5)
      |```
      |""".stripMargin,
    """|```scala
       |println("what!")
       |// what!
       |```
       |
       |```scala
       |def hello(x: Int) =
       |  if x != 0 then
       |    println(x)
       |    x / 0
       |hello(5)
       |// java.lang.ArithmeticException: / by zero
       |//  at repl.MdocSession$MdocApp.hello$1(significant-indentation.md:19)
       |//  at repl.MdocSession$MdocApp.$init$$$anonfun$1(significant-indentation.md:20)
       |//  at repl.MdocSession$MdocApp.$init$$$anonfun$adapted$1(significant-indentation.md:21)
       |```
       |""".stripMargin
  )

  check(
    "multiple-statements",
    """
      |```scala mdoc:crash
      |class Cat { def func = ??? }
      |(new Cat).func
      |```
      |""".stripMargin,
    """|```scala
       |class Cat { def func = ??? }
       |(new Cat).func
       |// scala.NotImplementedError: an implementation is missing
       |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:344)
       |// 	at repl.MdocSession$MdocApp$$anonfun$1$Cat$1.func(multiple-statements.md:9)
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(multiple-statements.md:10)
       |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(multiple-statements.md:8)
       |```
      """.stripMargin,
    compat = Map(
      Compat.Scala212 -> """|```scala
                            |class Cat { def func = ??? }
                            |(new Cat).func
                            |// scala.NotImplementedError: an implementation is missing
                            |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:344)
                            |// 	at repl.MdocSession$MdocApp$$anonfun$1$Cat$1.func(multiple-statements.md:9)
                            |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(multiple-statements.md:10)
                            |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(multiple-statements.md:8)
                            |```""".stripMargin,
      Compat.Scala211 -> """|```scala
                            |class Cat { def func = ??? }
                            |(new Cat).func
                            |// scala.NotImplementedError: an implementation is missing
                            |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:230)
                            |// 	at repl.MdocSession$MdocApp$$anonfun$1$Cat$1.func(multiple-statements.md:9)
                            |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(multiple-statements.md:10)
                            |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(multiple-statements.md:8)
                            |```""".stripMargin,
      Compat.Scala3 -> """|```scala
                          |class Cat { def func = ??? }
                          |(new Cat).func
                          |// scala.NotImplementedError: an implementation is missing
                          |// 	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:344)
                          |// 	at repl.MdocSession$MdocApp$$anonfun$1$Cat$1.func(multiple-statements.md:9)
                          |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(multiple-statements.md:10)
                          |// 	at repl.MdocSession$MdocApp$$anonfun$1.apply(multiple-statements.md:8)
                          |```""".stripMargin
    )
  )

}

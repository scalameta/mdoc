package tests.markdown

class NestSuite extends BaseMarkdownSuite {

  override def postProcessExpected: Map[Compat.ScalaVersion, String => String] =
    Map(
      Compat.Scala213 -> { old => old.replace("of type => Int", "of type Int") }
    )

  check(
    "redefine-val",
    """
      |```scala mdoc
      |val x = 1
      |val y = 1
      |println(x + y)
      |```
      |```scala mdoc:nest
      |val x = "x"
      |println(x + y)
      |```
      |```scala mdoc
      |println(x + y)
      |```
    """.stripMargin,
    """|
       |```scala
       |val x = 1
       |// x: Int = 1
       |val y = 1
       |// y: Int = 1
       |println(x + y)
       |// 2
       |```
       |```scala
       |val x = "x"
       |// x: String = "x"
       |println(x + y)
       |// x1
       |```
       |```scala
       |println(x + y)
       |// x1
       |```
       |""".stripMargin
  )

  check(
    "redefine-class",
    """
      |```scala mdoc
      |case class User(name: String)
      |val susan = User("Susan")
      |```
      |```scala mdoc:nest
      |case class User(name: String, age: Int)
      |val hakon = User("Hakon", 42)
      |susan
      |```
    """.stripMargin,
    """|
       |```scala
       |case class User(name: String)
       |val susan = User("Susan")
       |// susan: User = User("Susan")
       |```
       |```scala
       |case class User(name: String, age: Int)
       |val hakon = User("Hakon", 42)
       |// hakon: User = User("Hakon", 42)
       |susan
       |// res0: MdocApp.User = User("Susan")
       |```
       |""".stripMargin,
    compat = Map(
      Compat.Scala213 ->
        """|```scala
           |case class User(name: String)
           |val susan = User("Susan")
           |// susan: User = User(name = "Susan")
           |```
           |```scala
           |case class User(name: String, age: Int)
           |val hakon = User("Hakon", 42)
           |// hakon: User = User(name = "Hakon", age = 42)
           |susan
           |// res0: MdocApp.User = User(name = "Susan")
           |```
           |""".stripMargin,
      Compat.Scala3 ->
        """|```scala
           |case class User(name: String)
           |val susan = User("Susan")
           |// susan: User = User(name = "Susan")
           |```
           |```scala
           |case class User(name: String, age: Int)
           |val hakon = User("Hakon", 42)
           |// hakon: User = User(name = "Hakon", age = 42)
           |susan
           |// res0: User = User(name = "Susan")
           |```
           |""".stripMargin
    )
  )

  check(
    "multi-nest",
    """
      |```scala mdoc
      |val x = 1
      |val a = 1
      |```
      |```scala mdoc:nest
      |val y = "y"
      |val b = 2
      |```
      |```scala mdoc:nest
      |val z = List(1)
      |val c = 3
      |```
      |```scala mdoc
      |println(x)
      |println(y)
      |println(z)
      |```
    """.stripMargin,
    """|
       |```scala
       |val x = 1
       |// x: Int = 1
       |val a = 1
       |// a: Int = 1
       |```
       |```scala
       |val y = "y"
       |// y: String = "y"
       |val b = 2
       |// b: Int = 2
       |```
       |```scala
       |val z = List(1)
       |// z: List[Int] = List(1)
       |val c = 3
       |// c: Int = 3
       |```
       |```scala
       |println(x)
       |// 1
       |println(y)
       |// y
       |println(z)
       |// List(1)
       |```
       |""".stripMargin
  )

  check(
    "implicit-ok".tag(SkipScala3),
    """
      |```scala mdoc
      |implicit val x: Int = 1
      |```
      |```scala mdoc:nest
      |implicit val x: Int = 1
      |```
      |```scala mdoc
      |implicitly[Int]
      |```
    """.stripMargin,
    """|
       |```scala
       |implicit val x: Int = 1
       |// x: Int = 1
       |```
       |```scala
       |implicit val x: Int = 1
       |// x: Int = 1
       |```
       |```scala
       |implicitly[Int]
       |// res0: Int = 1
       |```
       |
       |""".stripMargin
  )

  check(
    "implicit-ok-scala3".tag(OnlyScala3),
    """
      |```scala mdoc
      |implicit val x: Int = 1
      |```
      |```scala mdoc:nest
      |implicit val x: Int = 1
      |```
      |```scala mdoc
      |implicitly[Int]
      |```
    """.stripMargin,
    """|
       |```scala
       |implicit val x: Int = 1
       |// x: Int = 1
       |```
       |```scala
       |implicit val x: Int = 1
       |// x: Int = 1
       |```
       |```scala
       |implicitly[Int]
       |// res0: Int = 1
       |```
       |
       |""".stripMargin
  )

  checkError(
    "reset".tag(SkipScala3),
    """
      |```scala mdoc
      |implicit val x: Int = 1
      |```
      |```scala mdoc:nest
      |implicit val x: Int = 1
      |```
      |```scala mdoc:reset
      |implicitly[Int]
      |```
    """.stripMargin,
    """|error: reset.md:9:1: could not find implicit value for parameter e: Int
       |implicitly[Int]
       |^^^^^^^^^^^^^^^
       |""".stripMargin
  )

  checkError(
    "reset-scala3".tag(OnlyScala3),
    """
      |```scala mdoc
      |implicit val x: Int = 1
      |```
      |```scala mdoc:nest
      |implicit val x: Int = 1
      |```
      |```scala mdoc:reset
      |implicitly[Int]
      |```
    """.stripMargin,
    """
      |error: reset-scala3.md:9:15:
      |No given instance of type Int was found for parameter e of method implicitly in object Predef
      |implicitly[Int]
      |              ^
         """.stripMargin
  )

  checkError(
    "multi-reset-scala3".tag(OnlyScala3),
    """
      |```scala mdoc
      |implicit val x: Int = 1
      |```
      |```scala mdoc:nest
      |implicit val x = 1
      |```
      |```scala mdoc:nest
      |implicit val x = 1
      |```
      |```scala mdoc:nest
      |implicit val x = 1
      |```
      |```scala mdoc:reset
      |implicitly[Int]
      |```
      |```scala mdoc:nest
      |implicit val x = 1
      |```
      |```scala mdoc:nest
      |implicit val x = 1
      |```
      |```scala mdoc:nest
      |implicit val x = 1
      |```
      |```scala mdoc:reset
      |implicitly[Int]
      |```
    """.stripMargin,
    """
      |error: multi-reset-scala3.md:15:15:
      |No given instance of type Int was found for parameter e of method implicitly in object Predef
      |implicitly[Int]
      |              ^
      |error: multi-reset-scala3.md:27:15:
      |No given instance of type Int was found for parameter e of method implicitly in object Predef
      |implicitly[Int]
      |              ^
    """.trim.stripMargin
  )

  checkError(
    "multi-reset".tag(SkipScala3),
    """
      |```scala mdoc
      |implicit val x: Int = 1
      |```
      |```scala mdoc:nest
      |implicit val x: Int = 1
      |```
      |```scala mdoc:nest
      |implicit val x: Int = 1
      |```
      |```scala mdoc:nest
      |implicit val x: Int = 1
      |```
      |```scala mdoc:reset
      |implicitly[Int]
      |```
      |```scala mdoc:nest
      |implicit val x: Int = 1
      |```
      |```scala mdoc:nest
      |implicit val x: Int = 1
      |```
      |```scala mdoc:nest
      |implicit val x: Int = 1
      |```
      |```scala mdoc:reset
      |implicitly[Int]
      |```
    """.stripMargin,
    """|error: multi-reset.md:15:1: could not find implicit value for parameter e: Int
       |implicitly[Int]
       |^^^^^^^^^^^^^^^
       |error: multi-reset.md:27:1: could not find implicit value for parameter e: Int
       |implicitly[Int]
       |^^^^^^^^^^^^^^^
       |""".stripMargin
  )

  // This test is skipped for Scala 3 because of changes
  // in implicit resolution
  // See http://dotty.epfl.ch/docs/reference/changed-features/implicit-resolution.html
  checkError(
    "implicit-nok".tag(SkipScala3),
    """
      |```scala mdoc
      |implicit val x: Int = 1
      |```
      |```scala mdoc:nest
      |implicit val y: Int = 1
      |```
      |```scala mdoc
      |implicitly[Int]
      |```
    """.stripMargin,
    """|error: implicit-nok.md:9:1: ambiguous implicit values:
       | both value x in object MdocApp of type => Int
       | and value y of type Int
       | match expected type Int
       |implicitly[Int]
       |^^^^^^^^^^^^^^^
       |""".stripMargin
  )

  checkError(
    "anyval-nok",
    """
      |```scala mdoc:reset-object
      |val x = 1
      |```
      |```scala mdoc:nest
      |class Foo(val x: Int) extends AnyVal
      |```
    """.stripMargin,
    """|error: anyval-nok.md:6:1: value class may not be a local class
       |class Foo(val x: Int) extends AnyVal
       |^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|error: anyval-nok.md:6:7:
           |Value classes may not be a local class
           |class Foo(val x: Int) extends AnyVal
           |      ^
           |""".stripMargin,
      Compat.Scala213 ->
        """|error: anyval-nok.md:6:7: value class may not be a local class
           |class Foo(val x: Int) extends AnyVal
           |      ^^^
           |""".stripMargin
    )
  )

  checkError(
    "stacktrace",
    """
      |```scala mdoc
      |val x = 1
      |```
      |```scala mdoc:nest
      |val x = 2
      |```
      |```scala mdoc:nest
      |val x = 3
      |```
      |```scala mdoc:nest
      |val x = 4
      |def boom(cond: Boolean) = if (!cond) throw new IllegalArgumentException()
      |boom(x > 4)
      |```
    """.stripMargin,
    """|error: stacktrace.md:14:1: null
       |boom(x > 4)
       |^^^^^^^^^^^
       |java.lang.IllegalArgumentException
       |	at repl.MdocSession$MdocApp$.boom$1(stacktrace.md:32)
       |	at repl.MdocSession$MdocApp$.<init>(stacktrace.md:35)
       |	at repl.MdocSession$.app(stacktrace.md:3)
       |""".stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """|error: stacktrace.md:14:1: null
           |boom(x > 4)
           |^^^^^^^^^^^
           |java.lang.IllegalArgumentException
           |	at repl.MdocSession$MdocApp$.boom$1(stacktrace.md:32)
           |	at repl.MdocSession$MdocApp$.<init>(stacktrace.md:36)
           |	at repl.MdocSession$.app(stacktrace.md:3)
           |""".stripMargin,
      Compat.Scala212 ->
        """|error: stacktrace.md:14:1: null
           |boom(x > 4)
           |^^^^^^^^^^^
           |java.lang.IllegalArgumentException
           |	at repl.MdocSession$MdocApp$.boom$1(stacktrace.md:32)
           |	at repl.MdocSession$MdocApp$.<init>(stacktrace.md:35)
           |	at repl.MdocSession$MdocApp$.<clinit>(stacktrace.md)
           |	at repl.MdocSession$.app(stacktrace.md:3)
           |""".stripMargin
    )
  )

  check(
    "fail",
    """
      |```scala mdoc
      |case class Foo(i: Int)
      |```
      |
      |```scala mdoc:nest:fail
      |case class Foo(i: Int) { val x = y }
      |```
    """.stripMargin,
    """|
       |```scala
       |case class Foo(i: Int)
       |```
       |
       |```scala
       |case class Foo(i: Int) { val x = y }
       |// error: not found: value y
       |// case class Foo(i: Int) { val x = y }
       |//                                  ^
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
        """
          |error:
          |Not found: y 
    """.stripMargin
    )
  )

}

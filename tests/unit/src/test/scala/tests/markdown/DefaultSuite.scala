package tests.markdown

class DefaultSuite extends BaseMarkdownSuite {

  check(
    "one",
    """
      |```scala mdoc
      |val x = List(1).map(_ + 1)
      |```
    """.stripMargin,
    """|```scala
       |val x = List(1).map(_ + 1)
       |// x: List[Int] = List(2)
       |```
    """.stripMargin
  )

  check(
    "two",
    """
      |# Hey Scala!
      |
      |```scala mdoc
      |val xs = List(1, 2, 3)
      |```
      |
      |```scala mdoc
      |val ys = xs.map(_ * 2)
      |```
    """.stripMargin,
    """|# Hey Scala!
       |
       |```scala
       |val xs = List(1, 2, 3)
       |// xs: List[Int] = List(1, 2, 3)
       |```
       |
       |```scala
       |val ys = xs.map(_ * 2)
       |// ys: List[Int] = List(2, 4, 6)
       |```
    """.stripMargin
  )

  check(
    "res0",
    """
      |```scala mdoc
      |List(1).map(_ + 1)
      |res0.length
      |```
      |
      |```scala mdoc
      |println(1)
      |```
      """.stripMargin,
    """|```scala
       |List(1).map(_ + 1)
       |// res0: List[Int] = List(2)
       |res0.length
       |// res1: Int = 1
       |```
       |
       |```scala
       |println(1)
       |// 1
       |```
      """.stripMargin
  )

  check(
    "lazy-val",
    """
      |```scala mdoc
      |lazy val x = ???
      |List(1).map(_ + 1)
      |res0.length
      |```
      |
      |```scala mdoc
      |println(1)
      |```
    """.stripMargin,
    """|```scala
       |lazy val x = ???
       |List(1).map(_ + 1)
       |// res0: List[Int] = List(2)
       |res0.length
       |// res1: Int = 1
       |```
       |
       |```scala
       |println(1)
       |// 1
       |```
    """.stripMargin
  )

  check(
    "lazy-val 2",
    """
      |```scala mdoc
      |lazy val x = 3
      |List(1).map(_ + x)
      |res0.length
      |```
      |
      |```scala mdoc
      |println(1)
      |```
    """.stripMargin,
    """|```scala
       |lazy val x = 3
       |List(1).map(_ + x)
       |// res0: List[Int] = List(4)
       |res0.length
       |// res1: Int = 1
       |```
       |
       |```scala
       |println(1)
       |// 1
       |```
    """.stripMargin
  )

  check(
    "defn",
    """
      |```scala mdoc
      |case class User(name: String, age: Int)
      |User("John", 42)
      |```
    """.stripMargin,
    """|```scala
       |case class User(name: String, age: Int)
       |User("John", 42)
       |// res0: User = User("John", 42)
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala213 ->
        """|```scala
           |case class User(name: String, age: Int)
           |User("John", 42)
           |// res0: User = User(name = "John", age = 42)
           |```
           |""".stripMargin,
      Compat.Scala3 ->
        """|```scala
           |case class User(name: String, age: Int)
           |User("John", 42)
           |// res0: User = User(name = "John", age = 42)
           |```
           |""".stripMargin
    )
  )

  check(
    "import",
    """
      |```scala mdoc
      |import scala.concurrent.Future
      |Future.successful(1)
      |```
    """.stripMargin,
    """|```scala
       |import scala.concurrent.Future
       |Future.successful(1)
       |// res0: Future[Int] = Future(Success(1))
       |```
    """.stripMargin
  )

  check(
    "many",
    """
      |```scala mdoc
      |println(1)
      |val x = 42
      |```
      |
      |```scala mdoc
      |println(x)
      |```
    """.stripMargin.replace("'''", "\"\"\""),
    """|```scala
       |println(1)
       |// 1
       |val x = 42
       |// x: Int = 42
       |```
       |
       |```scala
       |println(x)
       |// 42
       |```
    """.stripMargin
  )

  check(
    "blank-line",
    """
      |```scala mdoc
      |import scala.util._
      |import scala.math._
      |
      |import scala.concurrent._
      |Future.successful(Try(1))
      |val (a, b) = Future.successful(Try(1)) -> 2
      |
      |Future.successful(Try(1))
      |
      |// penultimate
      |```
    """.stripMargin,
    """|```scala
       |import scala.util._
       |import scala.math._
       |
       |import scala.concurrent._
       |Future.successful(Try(1))
       |// res0: Future[Try[Int]] = Future(Success(Success(1)))
       |val (a, b) = Future.successful(Try(1)) -> 2
       |// a: Future[Try[Int]] = Future(Success(Success(1)))
       |// b: Int = 2
       |
       |Future.successful(Try(1))
       |// res1: Future[Try[Int]] = Future(Success(Success(1)))
       |
       |// penultimate
       |```
    """.stripMargin
  )

  check(
    "large-value",
    """
      |```scala mdoc
      |Array.tabulate(10, 10)((a, b) => a + b)
      |```
    """.stripMargin.replace("'''", "\"\"\""),
    """|```scala
       |Array.tabulate(10, 10)((a, b) => a + b)
       |// res0: Array[Array[Int]] = Array(
       |//   Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9),
       |//   Array(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
       |//   Array(2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
       |//   Array(3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
       |//   Array(4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
       |//   Array(5, 6, 7, 8, 9, 10, 11, 12, 13, 14),
       |//   Array(6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
       |//   Array(7, 8, 9, 10, 11, 12, 13, 14, 15, 16),
       |//   Array(8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
       |//   Array(9, 10, 11, 12, 13, 14, 15, 16, 17, 18)
       |// )
       |```
    """.stripMargin
  )

  check(
    "leading-comment",
    """
      |```scala mdoc
      |
      |/* Comment 1 */
      |val x = 2
      |
      |// Comment 2
      |
      |val y = 2
      |
      |// Comment 4
      |x + y
      |
      |/** Docstring */
      |class User()
      |
      |// ultimate```
    """.stripMargin,
    """|```scala
       |/* Comment 1 */
       |val x = 2
       |// x: Int = 2
       |
       |// Comment 2
       |
       |val y = 2
       |// y: Int = 2
       |
       |// Comment 4
       |x + y
       |// res0: Int = 4
       |
       |/** Docstring */
       |class User()
       |
       |// ultimate```
    """.stripMargin
  )

  check(
    "backtick",
    // see https://github.com/scalameta/mdoc/issues/97
    """
      |```scala mdoc
      |type `0` = Int
      |```
    """.stripMargin,
    """|```scala
       |type `0` = Int
       |```
       |""".stripMargin
  )

  check(
    "backtick",
    // see https://github.com/scalameta/mdoc/issues/97
    """
      |```scala mdoc
      |type `0` = Int
      |```
    """.stripMargin,
    """|```scala
       |type `0` = Int
       |```
       |""".stripMargin
  )

  checkError(
    "fatal-exception",
    // see https://github.com/scalameta/metals/issues/1456
    """
      |```scala mdoc
      |throw new StackOverflowError()
      |```
    """.stripMargin,
    """|error: fatal-exception.md:3:1: null
       |throw new StackOverflowError()
       |^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
       |java.lang.StackOverflowError
       |	at repl.MdocSession$MdocApp.<init>(fatal-exception.md:8)
       |	at repl.MdocSession$.app(fatal-exception.md:3)
       |""".stripMargin
  )

  check(
    "infix",
    """
      |```scala mdoc
      |import scala.language.postfixOps
      |42 toString
      |```
    """.stripMargin,
    """|```scala
       |import scala.language.postfixOps
       |42 toString
       |// res0: String = "42"
       |```
       |""".stripMargin
  )
}

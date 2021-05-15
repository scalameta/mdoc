package tests.markdown

class SemanticsSuite extends BaseMarkdownSuite {

  check(
    "overload",
    """
      |```scala mdoc
      |def add(a: Int, b: Int): Int = a + b
      |def add(a: Int): Int = add(a, 1)
      |add(3)
      |```
    """.stripMargin,
    """|```scala
       |def add(a: Int, b: Int): Int = a + b
       |def add(a: Int): Int = add(a, 1)
       |add(3)
       |// res0: Int = 4
       |```
    """.stripMargin
  )

  check(
    "companion",
    """
      |```scala mdoc
      |case class User(name: String)
      |object User {
      |  implicit val ordering: Ordering[User] = Ordering.by(_.name)
      |}
      |List(User("Susan"), User("John")).sorted
      |```
    """.stripMargin,
    """|```scala
       |case class User(name: String)
       |object User {
       |  implicit val ordering: Ordering[User] = Ordering.by(_.name)
       |}
       |List(User("Susan"), User("John")).sorted
       |// res0: List[User] = List(User("John"), User("Susan"))
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala213 ->
        """|```scala
           |case class User(name: String)
           |object User {
           |  implicit val ordering: Ordering[User] = Ordering.by(_.name)
           |}
           |List(User("Susan"), User("John")).sorted
           |// res0: List[User] = List(User(name = "John"), User(name = "Susan"))
           |```
           |""".stripMargin
    )
  )

  check(
    "mutually-recursive",
    """
      |```scala mdoc
      |def isEven(n: Int): Boolean = n == 0 || !isOdd(n - 1)
      |def isOdd(n: Int): Boolean  = n == 1 || !isEven(n - 1)
      |isEven(8)
      |```
    """.stripMargin,
    """|```scala
       |def isEven(n: Int): Boolean = n == 0 || !isOdd(n - 1)
       |def isOdd(n: Int): Boolean  = n == 1 || !isEven(n - 1)
       |isEven(8)
       |// res0: Boolean = false
       |```
    """.stripMargin
  )
}

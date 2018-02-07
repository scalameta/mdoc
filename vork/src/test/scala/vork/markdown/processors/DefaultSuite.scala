package vork.markdown.processors

class DefaultSuite extends BaseMarkdownSuite {

  check(
    "one",
    """
      |```scala vork
      |val x = List(1).map(_ + 1)
      |```
    """.stripMargin,
    """
      |```scala
      |@ val x = List(1).map(_ + 1)
      |x: List[Int] = List(2)
      |```
    """.stripMargin
  )

  check(
    "two",
    """
      |# Hey Scala!
      |
      |```scala vork
      |val xs = List(1, 2, 3)
      |```
      |
      |```scala vork
      |val ys = xs.map(_ * 2)
      |```
    """.stripMargin,
    """
      |# Hey Scala!
      |
      |```scala
      |@ val xs = List(1, 2, 3)
      |xs: List[Int] = List(1, 2, 3)
      |```
      |
      |```scala
      |@ val ys = xs.map(_ * 2)
      |ys: List[Int] = List(2, 4, 6)
      |```
    """.stripMargin
  )
}

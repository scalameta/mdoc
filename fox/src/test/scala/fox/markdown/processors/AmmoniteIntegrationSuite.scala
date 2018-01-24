package fox.markdown.processors

class AmmoniteIntegrationSuite extends BaseMarkdownSuite {
  check(
    """
      |# Hey Scala!
      |
      |```scala
      |val xs = List(1, 2, 3)
      |val ys = xs.map(_ + 1)
      |```
      |
      |```scala
      |val zs = ys.map(_ * 2)
      |```
    """.stripMargin,
    """
      |# Hey Scala!
      |
      |```scala
      |@ val xs = List(1, 2, 3)
      |xs: List[Int] = List(1, 2, 3)
      |@ val ys = xs.map(_ + 1)
      |ys: List[Int] = List(2, 3, 4)
      |```
      |
      |```scala
      |@ val zs = ys.map(_ * 2)
      |zs: List[Int] = List(4, 6, 8)
      |```
    """.stripMargin
  )
}

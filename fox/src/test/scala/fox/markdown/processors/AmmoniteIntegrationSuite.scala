package fox.markdown.processors

class AmmoniteIntegrationSuite extends BaseMarkdownSuite {
  check(
    "code",
    """
      |# Hey Scala!
      |
      |```scala fox
      |val xs = List(1, 2, 3)
      |val ys = xs.map(_ + 1)
      |```
      |
      |```scala fox
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

  check(
    "passthrough",
    """
      |```scala fox:passthrough
      |println("# Header\n\nparagraph\n\n* bullet")
      |```
    """.stripMargin,
    """
      |# Header
      |
      |paragraph
      |
      |* bullet
    """.stripMargin
  )

  check(
    "fail",
    """
      |```scala fox:fail
      |val x: Int = "String"
      |```
    """.stripMargin,
    """
      |```scala
      |@ val x: Int = "String"
      |cmd0.sc:1: type mismatch;
      | found   : String("String")
      | required: Int
      |val x: Int = "String"
      |             ^
      |Compilation Failed
      |```
    """.stripMargin
  )

  checkError[CodeFenceError](
    "fail-error",
    """
      |```scala fox
      |foobar
      |```
    """.stripMargin
  )

  checkError[CodeFenceSuccess](
    "fail-success",
    """
      |```scala fox:fail
      |1.to(2)
      |```
    """.stripMargin
  )

}

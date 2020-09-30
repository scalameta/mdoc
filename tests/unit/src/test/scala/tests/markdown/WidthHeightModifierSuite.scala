package tests.markdown

class WidthHeightModifierSuite extends BaseMarkdownSuite {

  check(
    "no-width-override",
    """
      |```scala mdoc
      |List.fill(2)(List(1,2,3,4,5))
      |```
    """.stripMargin,
    """
      |```scala
      |List.fill(2)(List(1,2,3,4,5))
      |// res0: List[List[Int]] = List(List(1, 2, 3, 4, 5), List(1, 2, 3, 4, 5))
      |```      
    """.stripMargin
  )

  check(
    "width-override",
    """
      |```scala mdoc:width=20
      |List.fill(2)(List(1,2,3,4,5))
      |```
    """.stripMargin,
    """
      |```scala
      |List.fill(2)(List(1,2,3,4,5))
      |// res0: List[List[Int]] = List(
      |//   List(
      |//     1,
      |//     2,
      |//     3,
      |//     4,
      |//     5
      |//   ),
      |//   List(
      |//     1,
      |//     2,
      |//     3,
      |//     4,
      |//     5
      |//   )
      |// )
      |```
    """.stripMargin
  )

  check(
    "height-override",
    """
      |```scala mdoc:height=5
      |List.fill(15)("hello world!")
      |```
    """.stripMargin,
    """
      |```scala
      |List.fill(15)("hello world!")
      |// res0: List[String] = List(
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |// ...
      |```
    """.stripMargin
  )

  check(
    "no-height-override",
    """
      |```scala mdoc
      |List.fill(15)("hello world!")
      |```
    """.stripMargin,
    """
      |```scala
      |List.fill(15)("hello world!")
      |// res0: List[String] = List(
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!",
      |//   "hello world!"
      |// )
      |```
    """.stripMargin
  )

}

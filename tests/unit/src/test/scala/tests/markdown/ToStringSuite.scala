package tests.markdown

class ToStringSuite extends BaseMarkdownSuite {
  check(
    "basic",
    """
      |```scala mdoc:to-string
      |List("a")
      |```
      |```scala mdoc
      |List("a")
      |```
    """.stripMargin,
    """|
       |```scala
       |List("a")
       |// res0: List[String] = List(a)
       |```
       |
       |```scala
       |List("a")
       |// res1: List[String] = List("a")
       |```
    """.stripMargin
  )

}

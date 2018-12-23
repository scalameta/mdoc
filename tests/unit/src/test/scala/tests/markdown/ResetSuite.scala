package tests.markdown

class ResetSuite extends BaseMarkdownSuite {

  check(
    "basic",
    """
      |```scala mdoc
      |implicit val x: Int = 42
      |```
      |
      |```scala mdoc:reset
      |implicit val y: Int = 42
      |println(implicitly[Int])
      |```
      |
      |```scala mdoc:fail
      |println(x)
      |```
    """.stripMargin,
    """|```scala
       |implicit val x: Int = 42
       |// x: Int = 42
       |```
       |
       |```scala
       |implicit val y: Int = 42
       |// y: Int = 42
       |println(implicitly[Int])
       |// 42
       |```
       |
       |```scala
       |println(x)
       |// not found: value x
       |// println(x)
       |//         ^
       |```
    """.stripMargin
  )

}

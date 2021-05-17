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
       |// error: not found: value x
       |// println(x)
       |//         ^
       |```
    """.stripMargin,
    compat = Map(
      Compat.Scala3 ->
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
           |// error:
           |// Not found: x
           |// println(x)
           |//         ^
           |```
    """.stripMargin
    )
  )

  check(
    "empty",
    """
      |```scala mdoc
      |implicit val x: Int = 42
      |```
      |
      |```scala mdoc:reset
      |```
      |
      |```scala mdoc
      |implicit val x: Int = 41
      |println(x)
      |```
    """.stripMargin,
    """|```scala
       |implicit val x: Int = 42
       |// x: Int = 42
       |```
       |
       |```scala
       |
       |```
       |
       |```scala
       |implicit val x: Int = 41
       |// x: Int = 41
       |println(x)
       |// 41
       |```
    """.stripMargin
  )

}

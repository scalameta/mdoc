package tests.markdown

class Scala3Suite extends BaseMarkdownSuite {
  check(
    "three space indent",
    """
      |```scala mdoc
      |val x = 25
      |def test =
      |   println(x + 1)
      |   if x > 25 then
      |      println("yep")
      |   else
      |      println("nope")
      |test
      |```
      |
      |```scala mdoc:nest
      |def test =
      |   println("howdy!")
      |
      |test
      |```
    """.stripMargin,
    """
      |```scala
      |val x = 25
      |// x: Int = 25
      |def test =
      |   println(x + 1)
      |   if x > 25 then
      |      println("yep")
      |   else
      |      println("nope")
      |test
      |// 26
      |// nope
      |```
      |
      |```scala
      |def test =
      |   println("howdy!")
      |
      |test
      |// howdy!
      |```
""".stripMargin
  )
}

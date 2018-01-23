package fox.markdown.processors

import fox.{Markdown, Options}
import scala.meta.testkit.DiffAssertions

class AmmonitePostProcessorTest extends org.scalatest.FunSuite with DiffAssertions {
  private val options = new Options()
  def check(original: String, expected: String): Unit = {
    test(original) {
      val obtained = Markdown.toMarkdown(original, options)
      assertNoDiff(obtained, expected)
    }
  }

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

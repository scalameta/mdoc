package tests.markdown

import org.scalatest.Ignore

@Ignore
class ReplSuite extends BaseMarkdownSuite {
  check(
    "shadow",
    """
      |```scala vork:passthrough
      |val x = 1
      |val x = 2
      |println("Number " + (x * 3))
      |```
      |
      |```scala vork:passthrough
      |println("Number " + (x * 6))
      |```
      |
    """.stripMargin,
    """
      |Number 6
      |
      |Number 12
    """.stripMargin
  )

}

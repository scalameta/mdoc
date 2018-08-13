package vork.markdown.processors

class PassthroughSuite extends BaseMarkdownSuite {
  check(
    "passthrough",
    // TODO(olafur) handle non-val, test fails if we remove `val x =`
    """
      |```scala vork:passthrough
      |val x = println("# Header\n\nparagraph\n\n* bullet")
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
}

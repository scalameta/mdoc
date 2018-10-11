package tests.markdown

class InvisibleSuite extends BaseMarkdownSuite {

  check(
    "basic",
    """
      |```scala mdoc:invisible
      |val msg = "Hello!"
      |println(msg)
      |```
    """.stripMargin,
    ""
  )

}

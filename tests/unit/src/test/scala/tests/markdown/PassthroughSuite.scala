package tests.markdown

class PassthroughSuite extends BaseMarkdownSuite {

  check(
    "passthrough",
    """
      |```scala mdoc:passthrough
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

  check(
    "no-val",
    """
      |```scala mdoc:passthrough
      |println("# Header")
      |```
    """.stripMargin,
    """
      |# Header
    """.stripMargin
  )

  check(
    "stripMargin",
    """
```scala mdoc:passthrough
println('''|* Bullet 1
           |* Bullet 2
     |* Bullet 3
        '''.stripMargin)
```
    """.replace("'''", "\"\"\""),
    """
* Bullet 1
* Bullet 2
* Bullet 3
    """
  )

}

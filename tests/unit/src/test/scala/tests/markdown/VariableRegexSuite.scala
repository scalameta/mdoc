package tests.markdown

class VariableRegexSuite extends BaseMarkdownSuite {

  check(
    "basic",
    """
      |```scala
      |libraryDeps += "@version@"
      |```
    """.stripMargin,
    """
      |```scala
      |libraryDeps += "1.0"
      |```
    """.stripMargin,
  )

  checkError(
    "missing-key",
    """
      |```scala
      |libraryDeps += "@unknown@"
      |```
    """.stripMargin,
    """
      |error: missing-key.md:3:17: error: key not found: unknown
      |libraryDeps += "@unknown@"
      |                ^^^^^^^^^
    """.stripMargin,
  )

  check(
    "eval",
    """
      |```scala mdoc
      |val x = "@version@"
      |x.length // should match "1.0".length
      |```
    """.stripMargin,
    """
      |```scala
      |val x = "1.0"
      |// x: String = "1.0"
      |
      |x.length
      |// res0: Int = 3
      |```
    """.stripMargin,
  )

  check(
    "tag",
    """
      |This to @olafurpg
    """.stripMargin,
    """
      |This to @olafurpg
    """.stripMargin,
  )

  check(
    "tag-2",
    """
      |This to @@version@
    """.stripMargin,
    """
      |This to @version@
    """.stripMargin,
  )

}

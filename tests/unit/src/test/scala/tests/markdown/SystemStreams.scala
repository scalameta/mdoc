package tests.markdown

class SystemStreamsSuite extends BaseMarkdownSuite {

  check(
    "System.out",
    """
      |```scala mdoc
      |System.out.println("hello out")
      |```
      """.stripMargin,
    """
      |```scala
      |System.out.println("hello out")
      |// hello out
      |```
      """.stripMargin
  )

  check(
    "System.err",
    """
      |```scala mdoc
      |System.err.println("hello err")
      |```
      """.stripMargin,
    """
      |```scala
      |System.err.println("hello err")
      |// hello err
      |```
      """.stripMargin
  )

}

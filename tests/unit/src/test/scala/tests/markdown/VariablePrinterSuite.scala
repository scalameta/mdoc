package tests.markdown

class VariablePrinterSuite extends BaseMarkdownSuite {

  val trailingComment = baseSettings.copy(variablePrinter = { variable =>
    variable.runtimeValue match {
      case n: Int if variable.totalVariablesInStatement == 1 => s" // Number($n)"
      case _ => variable.toString
    }
  })

  check(
    "trailing-comment",
    """
      |```scala mdoc
      |42
      |"message"
      |val (a, b) = (1, 2)
      |```
    """.stripMargin,
    """|```scala
       |42 // Number(42)
       |"message"
       |// res1: String = "message"
       |val (a, b) = (1, 2)
       |// a: Int = 1
       |// b: Int = 2
       |```
    """.stripMargin,
    trailingComment
  )

  val lastStatementOnly = baseSettings.copy(variablePrinter = { variable =>
    if (variable.indexOfStatementInCodeFence + 1 == variable.totalStatementsInCodeFence) {
      variable.toString
    } else {
      ""
    }
  })
  check(
    "last-statement",
    """
      |```scala mdoc
      |val x = 12
      |val y = 2
      |x + y
      |x + y
      |```
    """.stripMargin,
    """|```scala
       |val x = 12
       |val y = 2
       |x + y
       |x + y
       |// res1: Int = 14
       |```
    """.stripMargin,
    lastStatementOnly
  )

}

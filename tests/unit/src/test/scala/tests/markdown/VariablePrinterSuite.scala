package tests.markdown
import mdoc.internal.markdown.ReplVariablePrinter

class VariablePrinterSuite extends BaseMarkdownSuite {

  check(
    "single-line-comment",
    """
      |```scala mdoc
      |import scala.Some // an import statement
      |val a = Some(1) // a variable
      |val b = 2 // another variable
      |```
    """.stripMargin,
    """|```scala
       |import scala.Some // an import statement
       |val a = Some(1) // a variable
       |// a: Some[Int] = Some(value = 1)
       |val b = 2 // another variable
       |// b: Int = 2
       |```
    """.stripMargin,
    baseSettings
  )

  check(
    "single-line-comment:compile-only",
    """|```scala mdoc:compile-only
       |// a
       |val a = 10
       |val b = 20 // b
       |val c = 30
       |```""".stripMargin,
    """|```scala
       |// a
       |val a = 10
       |val b = 20 // b
       |val c = 30
       |```
    """.stripMargin,
    baseSettings
  )

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

  val initialOffset = baseSettings.copy(
    variablePrinter =
      new ReplVariablePrinter(leadingNewline = true, width = 25, height = 20, indent = 2)
  )
  check(
    "initial-offset",
    """
      |```scala mdoc
      |// Column 25             |
      |val name = List(1, 2)
      |```
    """.stripMargin,
    """|```scala
       |// Column 25             |
       |val name = List(1, 2)
       |// name: List[Int] = List(
       |//   1,
       |//   2
       |// )
       |```
    """.stripMargin,
    initialOffset
  )
}

package tests.markdown

import tests.markdown.StringSyntax._
import tests.markdown.Compat

class FailPositionSuite extends BaseMarkdownSuite {

  check(
    "position",
    """|```scala mdoc
       |extension (x: Int)
       |    def ===(y: Int) = x == y
       |```
       |
       |```scala mdoc:fail
       |1 === ""
       |```
       |
       |```scala mdoc:silent
       | 1 === 1    
       |```
    """.stripMargin,
    """|```scala
       |extension (x: Int)
       |    def ===(y: Int) = x == y
       |```
       |
       |```scala
       |1 === ""
       |// error:
       |// Found:    ("" : String)
       |// Required: Int
       |// 1 === ""
       |//       ^^
       |```
       |
       |```scala
       | 1 === 1    
       |```
    """.stripMargin
  )

  check(
    "macro-call-after-similar-call",
    """|```scala mdoc
       |inline def operation(inline query: String): String =
       |  inline if query == "ok" then query else scala.compiletime.error("invalid query")
       |```
       |
       |```scala mdoc
       |operation("ok")
       |```
       |
       |```scala mdoc:fail
       |operation("not ok")
       |operation("bad")
       |```
       |
       |```scala mdoc:silent
       |operation("ok")
       |```
    """.stripMargin,
    """|```scala
       |inline def operation(inline query: String): String =
       |  inline if query == "ok" then query else scala.compiletime.error("invalid query")
       |```
       |
       |```scala
       |operation("ok")
       |// res0: String = "ok"
       |```
       |
       |```scala
       |operation("not ok")
       |operation("bad")
       |// error:
       |// invalid query
       |// operation("not ok")
       |// ^^^^^^^^^^^^^^^^^^^
       |// error:
       |// invalid query
       |// operation("bad")
       |// ^^^^^^^^^^^^^^^^
       |```
       |
       |```scala
       |operation("ok")
       |```
    """.stripMargin
  )

}

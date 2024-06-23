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

}

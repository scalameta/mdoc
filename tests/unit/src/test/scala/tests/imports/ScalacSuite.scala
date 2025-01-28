package tests.imports

import tests.markdown.BaseMarkdownSuite
import tests.markdown.Compat

class ScalacSuite extends BaseMarkdownSuite {

  checkError(
    "import".tag(OnlyScala213),
    """
      |```scala mdoc
      |import $scalac.`-Wunused:imports -Xfatal-warnings`
      |import scala.util.Try
      |println(42)
      |```
      |""".stripMargin,
    """|error: No warnings can be incurred under -Werror.
       |warning: import.md:4:1: Unused import
       |import scala.util.Try
       |^^^^^^^^^^^^^^^^^^^^^
       |""".stripMargin,
    compat = Map(
      Compat.Scala213 ->
        """|error: No warnings can be incurred under -Werror.
           |warning: import.md:4:19: Unused import
           |import scala.util.Try
           |                  ^^^
           |""".stripMargin
    )
  )

  check(
    "no-import".tag(OnlyScala213),
    """
      |```scala mdoc
      |import scala.util.Try
      |println(42)
      |```
      |""".stripMargin,
    """|```scala
       |import scala.util.Try
       |println(42)
       |// 42
       |```
       |""".stripMargin
  )

  check(
    "import-fail".tag(OnlyScala213),
    """
      |```scala mdoc
      |import $scalac.`-Wunused:imports -Xfatal-warnings`
      |```
      |```scala mdoc:fail
      |import scala.util.Try
      |println(42)
      |```
      |""".stripMargin,
    // NOTE(olafur) I'm not sure if this is the ideal behavior, this test is
    // only to document the current behavior.
    """|```scala
       |import $scalac.`-Wunused:imports -Xfatal-warnings`
       |```
       |```scala
       |import scala.util.Try
       |println(42)
       |// error: No warnings can be incurred under -Werror.
       |```
       |""".stripMargin
  )
}

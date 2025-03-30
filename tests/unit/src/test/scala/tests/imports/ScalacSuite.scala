package tests.imports

import tests.markdown.BaseMarkdownSuite
import tests.markdown.Compat

class ScalacSuite extends BaseMarkdownSuite {

  for (
    (name, importStyle) <- Seq(
      "ammonite" -> "import $scalac.`-Wunused:imports -Xfatal-warnings`",
      "using" -> "//> using option -Wunused:imports -Xfatal-warnings"
    )
  )
    checkError(
      s"import-$name".tag(OnlyScala213),
      s"""
         |```scala mdoc
         |$importStyle
         |import scala.util.Try
         |println(42)
         |```
         |""".stripMargin,
      s"""|error: No warnings can be incurred under -Werror.
          |warning: import-$name.md:4:1: Unused import
          |import scala.util.Try
          |^^^^^^^^^^^^^^^^^^^^^
          |""".stripMargin,
      compat = Map(
        Compat.Scala213 ->
          s"""|error: No warnings can be incurred under -Werror.
              |warning: import-$name.md:4:19: Unused import
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

  for (
    (name, importStyle) <- Seq(
      "ammonite" -> "import $scalac.`-Wunused:imports -Xfatal-warnings`",
      "using" -> "//> using option -Wunused:imports -Xfatal-warnings"
    )
  )
    check(
      s"import-fail-$name".tag(OnlyScala213),
      s"""
         |```scala mdoc
         |$importStyle
         |```
         |```scala mdoc:fail
         |import scala.util.Try
         |println(42)
         |```
         |""".stripMargin,
      // NOTE(olafur) I'm not sure if this is the ideal behavior, this test is
      // only to document the current behavior.
      s"""|```scala
          |$importStyle
          |```
          |```scala
          |import scala.util.Try
          |println(42)
          |// error: No warnings can be incurred under -Werror.
          |```
          |""".stripMargin
    )
}

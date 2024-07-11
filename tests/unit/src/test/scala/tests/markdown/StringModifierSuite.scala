package tests.markdown

import scala.meta.inputs.Input
import scala.meta.inputs.Position
import mdoc.StringModifier
import mdoc.Reporter
import mdoc.internal.cli.Settings

class StringModifierSuite extends BaseMarkdownSuite {
  override def baseSettings(resourcePropertyFileName: String): Settings =
    super
      .baseSettings()
      .copy(
        stringModifiers = List(
          new StringModifier {
            override val name: String = "hello"
            override def process(info: String, code: Input, reporter: Reporter): String = {
              code.text.trim + " " + info
            }
          },
          new StringModifier {
            override val name: String = "reporter"
            override def process(info: String, code: Input, reporter: Reporter): String = {
              val length = code.text.trim.length
              val pos = Position.Range(code, 0, length)
              reporter.error(pos, "This is a message")
              "reported"
            }
          },
          new StringModifier {
            override val name: String = "exception"
            override def process(info: String, code: Input, reporter: Reporter): String = {
              throw new IllegalArgumentException(info)
            }
          }
        )
      )

  check(
    "hello-world",
    """
      |```scala mdoc:hello:world!
      |Hello
      |```
    """.stripMargin,
    """
      |Hello world!
    """.stripMargin
  )

  checkError(
    "reporter",
    """
      |```scala mdoc:reporter
      |hello
      |```
    """.stripMargin,
    """
      |error: reporter.md:3:1: This is a message
      |hello
      |^^^^^
    """.stripMargin
  )

  checkError(
    "exception",
    """
      |```scala mdoc:exception:boom
      |hello
      |```
    """.stripMargin,
    """
      |error: exception.md:3:1: mdoc:exception exception
      |hello
      |^^^^^
      |mdoc.internal.markdown.ModifierException: mdoc:exception exception
      |Caused by: java.lang.IllegalArgumentException: boom
      |	at tests.markdown.StringModifierSuite$$anon$3.process(StringModifierSuite.scala:33)
    """.stripMargin
  )

}

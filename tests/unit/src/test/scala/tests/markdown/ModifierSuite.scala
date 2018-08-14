package tests.markdown

import scala.meta.inputs.Input
import scala.meta.inputs.Position
import vork.CustomModifier
import vork.Reporter
import vork.internal.cli.Settings

class ModifierSuite extends BaseMarkdownSuite {
  override def settings: Settings = super.settings.copy(
    modifiers = List(
      new CustomModifier {
        override val name: String = "hello"
        override def process(info: String, code: Input, reporter: Reporter): String = {
          code.text.trim + " " + info
        }
      },
      new CustomModifier {
        override val name: String = "reporter"
        override def process(info: String, code: Input, reporter: Reporter): String = {
          val length = code.text.trim.length
          val pos = Position.Range(code, 0, length)
          reporter.error(pos, "This is a message")
          "reported"
        }
      },
      new CustomModifier {
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
      |```scala vork:hello:world!
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
      |```scala vork:reporter
      |hello
      |```
    """.stripMargin,
    """
      |error: reporter.md:3:1: error: This is a message
      |hello
      |^^^^^
    """.stripMargin
  )

  checkError(
    "exception",
    """
      |```scala vork:exception:boom
      |hello
      |```
    """.stripMargin,
    """
      |error: exception.md:3:1: error: exception
      |hello
      |^^^^^
      |vork.internal.markdown.CustomModifierException: exception
      |Caused by: java.lang.IllegalArgumentException: boom
      |	at tests.markdown.ModifierSuite$$anon$3.process(ModifierSuite.scala:30)
    """.stripMargin
  )

}

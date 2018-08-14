package tests.markdown

import scala.meta.inputs.Input
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

}

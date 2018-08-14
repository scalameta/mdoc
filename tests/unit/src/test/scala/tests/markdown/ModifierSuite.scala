package tests.markdown

import vork.internal.cli.CustomModifier
import vork.internal.cli.Settings

class ModifierSuite extends BaseMarkdownSuite {
  override def settings: Settings = super.settings.copy(
    modifiers = List(
      new CustomModifier {
        override val name: String = "hello"
        override def process(info: String, code: String): String = {
          code.trim + " " + info
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

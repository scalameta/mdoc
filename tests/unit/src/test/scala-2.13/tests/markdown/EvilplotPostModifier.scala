package tests.markdown

import mdoc.PostModifier
import mdoc.PostModifierContext

class EvilplotPostModifier extends PostModifier {
  val name = "evilplot"
  def process(ctx: PostModifierContext): String = ""
}

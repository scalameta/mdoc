package tests.markdown

import mdoc.{MainSettings, PostModifier, PostModifierContext, Exit}

class EvilplotPostModifier extends PostModifier {
  val name = "evilplot"

  def process(ctx: PostModifierContext): String = ""

  override def onStart(settings: MainSettings): Unit = ()

  override def onExit(exit: Exit): Unit = ()
}

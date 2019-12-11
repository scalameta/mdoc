package tests.markdown

import mdoc.PostModifier
import mdoc.PostModifierContext

class EvilplotPostModifier extends PostModifier {
  val name = "evilplot"
  def process(ctx: PostModifierContext): String = ""

  override def onStart(ctx: PostModifierContext): Unit = ()

  override def preProcess(ctx: PostModifierContext): Unit = ()

  override def postProcess(ctx: PostModifierContext): Unit = ()

  override def onExit(ctx: PostModifierContext): Unit = ()
}

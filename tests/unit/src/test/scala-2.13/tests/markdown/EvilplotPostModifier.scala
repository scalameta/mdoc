package tests.markdown

import mdoc.{MainSettings, PostModifier, PostModifierContext}
import mdoc.internal.cli.Exit

class EvilplotPostModifier extends PostModifier {
  val name = "evilplot"
  def process(ctx: PostModifierContext): String = ""

  override def onStart(settings: MainSettings): Unit = ()

  override def preProcess(ctx: PostModifierContext): Unit = ()

  override def postProcess(ctx: PostModifierContext): Unit = ()

  override def onExit(exit: Exit): Unit = ()
}

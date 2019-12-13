package tests.markdown

import mdoc.PostModifier
import mdoc.PostModifierContext
import mdoc.internal.cli.{Exit, Settings}

class EvilplotPostModifier extends PostModifier {
  val name = "evilplot"

  def process(ctx: PostModifierContext): String = ""

  override def onStart(settings: Settings): Unit = ()

  override def preProcess(ctx: PostModifierContext): Unit = ()

  override def postProcess(ctx: PostModifierContext): Unit = ()

  override def onExit(exit: Exit): Unit = ()
}

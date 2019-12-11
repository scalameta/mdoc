package mdoc.docs

import mdoc._
import mdoc.internal.cli.{Exit, Settings}

class LifeCycleModifier extends PostModifier {
  val name = "lifecycle"
  def process(ctx: PostModifierContext): String = {
    "NOT IMPLEMENTED"
  }

  override def onStart(settings: Settings): Unit = ()

  override def preProcess(ctx: PostModifierContext): Unit = ()

  override def postProcess(ctx: PostModifierContext): Unit = ()

  override def onExit(exit: Exit): Unit = ()
}

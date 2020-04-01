package tests.markdown

import mdoc.{Exit, MainSettings, PostModifier, PostModifierContext}

class BulletPostModifier extends PostModifier {
  val name = "bullet"
  override def process(ctx: PostModifierContext): String = {
    ctx.lastValue match {
      case n: Int =>
        1.to(n).map(i => s"$i. Bullet").mkString("\n")
      case els =>
        ctx.reporter.error(s"expected int runtime value. Obtained $els")
        ""
    }
  }

  override def onStart(settings: MainSettings): Unit = ()

  override def onExit(exit: Exit): Unit = ()

}

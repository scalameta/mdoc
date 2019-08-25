package tests.markdown

import mdoc.PostModifier
import mdoc.PostModifierContext

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
}

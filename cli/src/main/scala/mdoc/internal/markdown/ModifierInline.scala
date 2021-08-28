package mdoc.internal.markdown

import mdoc.StringModifier
import mdoc.internal.markdown.Mod._

/** An mdoc inline code modifier.
  *
  * Modifiers are parsed from inline code blocks like here
  *
  * `scala mdoc:passthrough println("# Header")`
  *
  * Currently, only supports parsing one modifier per code block.
  */
sealed abstract class ModifierInline(val mods: Set[Mod]) {
  def isDefault: Boolean = mods.isEmpty
  def isFailOrWarn: Boolean = isFail || isWarn
  def isFail: Boolean = mods(Fail)
  def isWarn: Boolean = mods(Warn)
}
object ModifierInline {
  object Default {
    def apply(): ModifierInline = Builtin(Set.empty)
  }
  object Fail {
    def unapply(m: ModifierInline): Boolean =
      m.isFailOrWarn
  }
  object Warn {
    def unapply(m: ModifierInline): Boolean =
      m.isWarn
  }

  def apply(string: String): Option[ModifierInline] = {
    val mods = string.split(":").map {
      case Mod(m) => Some(m)
      case _ => None
    }
    if (mods.forall(_.isDefined)) {
      Some(Builtin(mods.iterator.map(_.get).toSet))
    } else {
      None
    }
  }

  case class Builtin(override val mods: Set[Mod]) extends ModifierInline(mods)
  case class Str(mod: StringModifier, info: String) extends ModifierInline(Set.empty)
  case class Post(mod: mdoc.PostModifier, info: String) extends ModifierInline(Set.empty)
  case class Pre(mod: mdoc.PreModifier, info: String) extends ModifierInline(Set.empty)

}

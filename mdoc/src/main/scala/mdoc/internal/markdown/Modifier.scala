package mdoc.internal.markdown

import mdoc.StringModifier
import mdoc.internal.markdown.Mod._

/**
  * A mdoc code fence modifier.
  *
  * Modifiers are parsed from code blocks like here
  *
  * ```scala mdoc:passthrough
  * println("# Header")
  * ```
  *
  * Currently, only supports parsing one modifier per code block.
  */
sealed abstract class Modifier(mods: Set[Mod]) {
  def isDefault: Boolean = mods.isEmpty
  def isFail: Boolean = mods(Fail)
  def isPassthrough: Boolean = mods(Passthrough)
  def isString: Boolean = this.isInstanceOf[Modifier.Str]
  def isPost: Boolean = this.isInstanceOf[Modifier.Post]
  def isCrash: Boolean = mods(Crash)
  def isSilent: Boolean = mods(Silent)
  def isInvisible: Boolean = mods(Invisible)
  def isReset: Boolean = mods(Reset)
}
object Modifier {
  object Default {
    def apply(): Modifier = Builtin(Set.empty)
  }
  object Crash {
    def unapply(m: Modifier): Boolean =
      m.isCrash
  }
  object Fail {
    def unapply(m: Modifier): Boolean =
      m.isFail
  }
  object PrintVariable {
    def unapply(m: Modifier): Boolean =
      m.isDefault || m.isPassthrough || m.isReset
  }

  def apply(string: String): Option[Modifier] = {
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

  case class Builtin(mods: Set[Mod]) extends Modifier(mods)

  /** Render this code fence according to this string modifier */
  case class Str(mod: StringModifier, info: String) extends Modifier(Set.empty)

  /** Render this code fence according to this post modifier */
  case class Post(mod: mdoc.PostModifier, info: String) extends Modifier(Set.empty)

}

package mdoc.internal.markdown

import mdoc.StringModifier
import mdoc.internal.markdown.Mod._


sealed trait GenModifier {
  val mods: Set[Mod]
  def isDefault: Boolean = mods.isEmpty
  def isFailOrWarn: Boolean = isFail || isWarn
  def isFail: Boolean = mods(Fail)
  def isWarn: Boolean = mods(Warn)
}

/** A mdoc code fence modifier.
  *
  * Modifiers are parsed from code blocks like here
  *
  * ```scala mdoc:passthrough
  * println("# Header")
  * ```
  *
  * Currently, only supports parsing one modifier per code block.
  */
sealed abstract class Modifier(val mods: Set[Mod]) extends GenModifier {
  def isPassthrough: Boolean = mods(Passthrough)
  def isString: Boolean = this.isInstanceOf[Modifier.Str]
  def isPre: Boolean = this.isInstanceOf[Modifier.Pre]
  def isPost: Boolean = this.isInstanceOf[Modifier.Post]
  def isCrash: Boolean = mods(Crash)
  def isSilent: Boolean = mods(Silent)
  def isInvisible: Boolean = mods(Invisible)
  def isCompileOnly: Boolean = mods(CompileOnly)
  def isReset: Boolean = isResetClass || isResetObject
  def isResetClass: Boolean = mods(ResetClass) || mods(Reset)
  def isResetObject: Boolean = mods(ResetObject)
  def isNest: Boolean = mods(Nest)

  def widthOverride: Option[Int] =
    mods.collectFirst { case Width(value) =>
      value
    }

  def heightOverride: Option[Int] =
    mods.collectFirst { case Height(value) =>
      value
    }

  def isToString: Boolean = mods(ToString)
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
      m.isFailOrWarn
  }
  object Warn {
    def unapply(m: Modifier): Boolean =
      m.isWarn
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

  case class Builtin(override val mods: Set[Mod]) extends Modifier(mods)
  case class Str(mod: StringModifier, info: String) extends Modifier(Set.empty)
  case class Post(mod: mdoc.PostModifier, info: String) extends Modifier(Set.empty)
  case class Pre(mod: mdoc.PreModifier, info: String) extends Modifier(Set.empty)

}

/** An mdoc inline code modifier.
 *
 * Modifiers are parsed from inline code blocks like here
 *
 * `scala mdoc:passthrough println("# Header")`
 *
 * Currently, only supports parsing one modifier per code block.
 */
case class ModifierInline(val mods: Set[Mod]) extends GenModifier
object ModifierInline {
  object Default {
    def apply(): ModifierInline = ModifierInline(Set.empty)
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
      Some(ModifierInline(mods.iterator.map(_.get).toSet))
    } else {
      None
    }
  }

}

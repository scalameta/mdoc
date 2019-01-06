package mdoc.internal.markdown

sealed abstract class Mod extends Product with Serializable
object Mod {
  case object Fail extends Mod
  case object Crash extends Mod
  case object Silent extends Mod
  case object Passthrough extends Mod
  case object Invisible extends Mod
  case object Reset extends Mod
  case object ResetClass extends Mod {
    override def toString: String = "reset-class"
  }

  def all: List[Mod] = List(
    Passthrough,
    Invisible,
    Reset,
    ResetClass,
    Fail,
    Crash,
    Silent
  )
  def unapply(string: String): Option[Mod] = {
    all.find(_.toString.equalsIgnoreCase(string))
  }
}

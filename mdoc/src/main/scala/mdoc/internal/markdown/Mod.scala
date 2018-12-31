package mdoc.internal.markdown

sealed abstract class Mod extends Product with Serializable
object Mod {
  case object Fail extends Mod
  case object Crash extends Mod
  case object Silent extends Mod
  case object Passthrough extends Mod
  case object Invisible extends Mod
  case object Reset extends Mod

  def all: List[Mod] = List(
    Passthrough,
    Invisible,
    Reset,
    Fail,
    Crash,
    Silent
  )
  def unapply(string: String): Option[Mod] = {
    all.find(_.toString.equalsIgnoreCase(string))
  }
}

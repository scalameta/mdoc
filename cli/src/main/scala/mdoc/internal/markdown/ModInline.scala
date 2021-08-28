package mdoc.internal.markdown

import scala.util.Try

sealed abstract class ModInline extends Product with Serializable
object ModInline {
  // The default behavior will be CompileOnly, so we don't need that Mod
  case object Fail extends ModInline
  case object Warn extends ModInline
  // Since we're not actually running, Crash is not relevant
//  case object Crash extends ModInline

  def static: List[ModInline] =
    List(
      Fail,
      Warn,
      Crash,
    )

  def unapply(string: String): Option[ModInline] = {
    static.find(_.toString.equalsIgnoreCase(string))
  }
}

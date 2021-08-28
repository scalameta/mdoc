package mdoc.internal.markdown

import scala.util.Try

sealed abstract class ModInline extends Product with Serializable
object ModInline {
  case object Fail extends ModInline
  case object Warn extends ModInline
  case object Crash extends ModInline

  // This will be the default behavior now, so I don't _think_ it makes sense to keep here
//  case object CompileOnly extends ModInline {
//    override def toString: String = "compile-only"
//  }

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

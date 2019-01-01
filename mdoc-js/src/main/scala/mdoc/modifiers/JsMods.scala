package mdoc.modifiers

import mdoc.Reporter
import mdoc.internal.pos.PositionSyntax._
import scala.annotation.tailrec
import scala.meta.inputs.Input

class JsMods private (mods: Set[String]) {
  def isShared: Boolean = mods("shared")
  def isInvisible: Boolean = mods("invisible")
}

object JsMods {
  val all = Set("shared", "invisible")
  def parse(info: Input, reporter: Reporter): Option[JsMods] = {
    val text = info.text
    @tailrec def loop(from: Int, accum: Set[String]): Option[Set[String]] = {
      if (from >= text.length) Some(accum)
      else {
        val colon = text.indexOf(':', from)
        val mod = if (colon < 0) {
          text.substring(from)
        } else {
          text.substring(from, colon)
        }
        val isValid = all.contains(mod)
        if (isValid && colon < 0) {
          loop(text.length + 1, accum + mod)
        } else if (isValid) {
          loop(colon + 1, accum + mod)
        } else {
          reporter.error(info.toPosition.addStart(from), s"invalid modifier '$mod'")
          None
        }
      }
    }
    loop(0, Set.empty).map(new JsMods(_))
  }
}

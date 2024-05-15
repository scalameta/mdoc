package mdoc.modifiers

import mdoc.Reporter
import mdoc.internal.pos.PositionSyntax._
import scala.annotation.tailrec
import scala.meta.inputs.Input
import mdoc.parser.Text
import mdoc.internal.markdown.LinkHygiene

case class AllMods(jsMods: Option[JsMods], otherMods: Array[String])

class JsMods private (val mods: Set[String]) {
  def isShared: Boolean = mods("shared")
  def isInvisible: Boolean = mods("invisible")
  def isCompileOnly: Boolean = mods("compile-only")
  def isEntrypoint: Boolean = !isShared && !isCompileOnly
}

object JsMods {
  val validFences = Set("shared", "invisible", "compile-only")
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
        val isValid = validFences.contains(mod)
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
    loop(0, Set.empty).map(new JsMods(_)).flatMap { mods =>
      if (mods.isCompileOnly && mods.mods.size > 1) {
        val others = (mods.mods - "compile-only").mkString(", ")
        reporter.error(
          info.toPosition.addStart(0),
          s"compile-only cannot be used in combination with $others"
        )
        None
      } else {
        Some(mods)
      }
    }
  }

  def parse(fences: Text, info: Input, reporter: Reporter): AllMods = {
    val jsMods = parse(info, reporter)

    if (reporter.hasErrors) {
      AllMods(jsMods, Array[String]())
    } else {
      // We already know that the scala fences are valid.
      val fenceBlocks = fences.value.split(" ")
      val dropScala = fenceBlocks.tail // first entry is scala
      val remainingFences = dropScala.filterNot(_.contains("mdoc:js"))
      // println(remainingFences.mkString(" ", " ", ""))
      AllMods(jsMods, remainingFences)
    }
  }
}

package vork.markdown.processors

import vork.Context

/**
  * A vork code fence modifier.
  *
  * Modifiers are parsed from code blocks like here
  *
  * ```scala vork:passthrough
  * println("# Header")
  * ```
  *
  * Currently, only supports parsing one modifier per code block.
  */
sealed trait FencedCodeMod {
  import FencedCodeMod._
  def isDefault: Boolean = this == Default
  def isFail: Boolean = this == Fail
  def isPassthrough: Boolean = this == Passthrough
}
object FencedCodeMod {
  def all: List[FencedCodeMod] = List(Default, Passthrough, Fail)
  def apply(string: String): Option[FencedCodeMod] =
    all.find(_.toString.equalsIgnoreCase(string))

  /** Render output as if in a normal repl. */
  case object Default extends FencedCodeMod

  /** Expect error and fail build if code block succeeds. */
  case object Fail extends FencedCodeMod

  /** Render stdout as raw markdown and remove code block. */
  case object Passthrough extends FencedCodeMod

  def unapply(string: String)(implicit ctx: Context): Option[FencedCodeMod] = {
    if (!string.startsWith("scala vork")) None
    else {
      if (!string.contains(':')) Some(Default)
      else {
        val mode = string.stripPrefix("scala vork:")
        FencedCodeMod(mode).orElse {
          val msg = s"Invalid mode '$mode'. Expected one of ${all.mkString(", ")}"
          ctx.logger.error(msg)
          None
        }
      }
    }
  }

}

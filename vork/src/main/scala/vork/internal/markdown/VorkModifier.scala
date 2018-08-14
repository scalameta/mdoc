package vork.internal.markdown

import vork.CustomModifier

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
sealed trait VorkModifier {
  import VorkModifier._
  def isDefault: Boolean = this == Default
  def isFail: Boolean = this == Fail
  def isPassthrough: Boolean = this == Passthrough
  def isCustom: Boolean = this.isInstanceOf[Custom]
  def isCrash: Boolean = this == Crash
}
object VorkModifier {
  def all: List[VorkModifier] = List(Default, Passthrough, Fail, Crash)
  def apply(string: String): Option[VorkModifier] =
    all.find(_.toString.equalsIgnoreCase(string))

  /** Render output as if in a normal repl. */
  case object Default extends VorkModifier

  /** Expect error and fail build if code block succeeds. */
  case object Fail extends VorkModifier

  /** Expect a runtime exception from evaluating the block. */
  case object Crash extends VorkModifier

  /** Render stdout as raw markdown and remove code block. */
  case object Passthrough extends VorkModifier

  /** Render this code fence according to this custom modifier */
  case class Custom(mod: CustomModifier, info: String) extends VorkModifier

}

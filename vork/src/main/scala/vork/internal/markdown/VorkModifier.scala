package vork.internal.markdown

import vork.internal.cli.Context

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
}
object VorkModifier {
  def all: List[VorkModifier] = List(Default, Passthrough, Fail)
  def apply(string: String): Option[VorkModifier] =
    all.find(_.toString.equalsIgnoreCase(string))

  /** Render output as if in a normal repl. */
  case object Default extends VorkModifier

  /** Expect error and fail build if code block succeeds. */
  case object Fail extends VorkModifier

  /** Render stdout as raw markdown and remove code block. */
  case object Passthrough extends VorkModifier


}

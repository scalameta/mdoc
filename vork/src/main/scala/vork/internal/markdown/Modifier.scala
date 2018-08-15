package vork.internal.markdown

import vork.StringModifier

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
sealed trait Modifier {
  import Modifier._
  def isDefault: Boolean = this == Default
  def isFail: Boolean = this == Fail
  def isPassthrough: Boolean = this == Passthrough
  def isString: Boolean = this.isInstanceOf[Str]
  def isCrash: Boolean = this == Crash
}
object Modifier {
  def all: List[Modifier] = List(Default, Passthrough, Fail, Crash)
  def apply(string: String): Option[Modifier] =
    all.find(_.toString.equalsIgnoreCase(string))

  /** Render output as if in a normal repl. */
  case object Default extends Modifier

  /** Expect error and fail build if code block succeeds. */
  case object Fail extends Modifier

  /** Expect a runtime exception from evaluating the block. */
  case object Crash extends Modifier

  /** Render stdout as raw markdown and remove code block. */
  case object Passthrough extends Modifier

  /** Render this code fence according to this string modifier */
  case class Str(mod: StringModifier, info: String) extends Modifier

}

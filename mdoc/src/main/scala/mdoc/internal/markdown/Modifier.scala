package mdoc.internal.markdown

import mdoc.StringModifier

/**
  * A mdoc code fence modifier.
  *
  * Modifiers are parsed from code blocks like here
  *
  * ```scala mdoc:passthrough
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
  def isSilent: Boolean = this == Silent
}
object Modifier {
  def all: List[Modifier] = List(Default, Passthrough, Invisible, Fail, Crash, Silent)
  def apply(string: String): Option[Modifier] =
    all.find(_.toString.equalsIgnoreCase(string))

  /** Render output as if in a normal repl. */
  case object Default extends Modifier

  /** Expect error and fail build if code block succeeds. */
  case object Fail extends Modifier

  /** Expect a runtime exception from evaluating the block. */
  case object Crash extends Modifier

  /** Keep the input code fence unchanged, don't print out the evaluated output. */
  case object Silent extends Modifier

  /** Render stdout as raw markdown and remove code block. */
  case object Passthrough extends Modifier

  /** Do no render anything. */
  case object Invisible extends Modifier

  /** Render this code fence according to this string modifier */
  case class Str(mod: StringModifier, info: String) extends Modifier

  /** Render this code fence according to this post modifier */
  case class Post(mod: mdoc.PostModifier, info: String) extends Modifier

}

package fox.markdown.processors

/**
  * A fox code fence modifier.
  *
  * Modifiers are parsed from code blocks like here
  *
  * ```scala fox:passthrough
  * println("# Header")
  * ```
  *
  * Currently, only supports parsing one modifier per code block.
  */
sealed trait FencedCodeMod
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

  def unapply(string: String): Option[FencedCodeMod] = {
    if (!string.startsWith("scala fox")) None
    else {
      if (!string.contains(':')) Some(Default)
      else {
        val mode = string.stripPrefix("scala fox:")
        FencedCodeMod(mode).orElse {
          val msg = s"Invalid mode '$mode'. Expected one of ${all.mkString(", ")}"
          throw new IllegalArgumentException(msg.toString)
        }
      }
    }
  }

}

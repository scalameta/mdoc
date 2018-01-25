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
sealed trait Mod
object Mod {
  def all: List[Mod] = List(Default, Passthrough, Fail)
  def apply(string: String): Option[Mod] =
    all.find(_.toString.equalsIgnoreCase(string))

  /** Render output as if in a normal repl. */
  case object Default extends Mod

  /** Expect error and fail build if code block succeeds. */
  case object Fail extends Mod

  /** Render stdout as raw markdown and remove code block. */
  case object Passthrough extends Mod

  def unapply(string: String): Option[Mod] = {
    if (!string.startsWith("scala fox")) None
    else {
      if (!string.contains(':')) Some(Default)
      else {
        val mode = string.stripPrefix("scala fox:")
        Mod(mode).orElse {
          val msg = s"Invalid mode '$mode'. Expected one of ${all.mkString(", ")}"
          throw new IllegalArgumentException(msg.toString)
        }
      }
    }
  }

}

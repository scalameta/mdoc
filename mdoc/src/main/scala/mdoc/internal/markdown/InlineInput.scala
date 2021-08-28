package mdoc.internal.markdown

import scala.meta.inputs.Input
import scala.meta.inputs.Position
import mdoc.internal.cli.Context
import mdoc.internal.markdown.Modifier.Str
import mdoc.internal.markdown.Modifier.Default
import mdoc.internal.markdown.Modifier.Post
import mdoc.internal.markdown.Modifier.Pre

case class PreInlineInput(block: CodeFence, input: Input, mod: Pre) // TODO Delete?
case class StringInlineInput(block: InlineCode, input: Input, mod: Str)
case class ScalaInlineInput(block: InlineMdoc, input: Input, mod: ModifierInline)

class InlineInput(ctx: Context, baseInput: Input) {
  def getModifier(info: Text): Option[ModifierInline] = {
    val string = info.value.stripLineEnd
    println("InlineInput string: " + string)
    if (!string.startsWith("`scala mdoc")) None
    else {
      if (!string.contains(':')) {
        println("InlineInput Default modifiers")
        Some(ModifierInline.Default())
      }
      else {
        val mode = string.stripPrefix("`scala mdoc:")
        println("InlineInput custom mode: " + mode)
        ModifierInline(mode)
          .orElse {
            invalid(info, s"Invalid mode '$mode'")
            None
          }
      }
    }
  }

  private def invalid(info: Text, message: String): Unit = {
    val offset = "scala mdoc:".length
    val start = info.pos.start + offset
    val end = info.pos.end - 1
    val pos = Position.Range(baseInput, start, end)
    ctx.reporter.error(pos, message)
  }
  private def invalidCombination(info: Text, mod1: String, mod2: String): Boolean = {
    invalid(info, s"invalid combination of modifiers '$mod1' and '$mod2'")
    false
  }

  private def isValid(info: Text, mod: ModifierInline): Boolean = {
    true
    // TODO Pick out relevant logic below to restore this method
    /* if (mod.isFailOrWarn && mod.isCrash) {
      invalidCombination(info, "crash", "fail")
    } else if (mod.isSilent && mod.isInvisible) {
      invalidCombination(info, "silent", "invisible")
    } else if (mod.isReset && mod.isNest) {
      invalid(
        info,
        "the modifier 'nest' is redundant when used in combination with 'reset'. " +
          "To fix this error, remove 'nest'"
      )
      false
    } else if (mod.isCompileOnly) {
      val others = mod.mods - Mod.CompileOnly
      if (others.isEmpty) {
        true
      } else {
        val all = others.map(_.toString.toLowerCase).mkString(", ")
        invalid(
          info,
          s"""compile-only cannot be used in combination with $all"""
        )
        false
      }
    } else {
      true
    }
    */
  }
  def unapply(block: InlineMdoc): Option[ScalaInlineInput] = {
    println("InlineInput.unapply")
    getModifier(block.info) match {
      case Some(mod) =>
        if (isValid(block.info, mod)) {
          val input = Input.Slice(baseInput, block.body.pos.start, block.body.pos.end)
          Some(ScalaInlineInput(block, input, mod))
        } else {
          None
        }
      case _ => None
    }
  }
}

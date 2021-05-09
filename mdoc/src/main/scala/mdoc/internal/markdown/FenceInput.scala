package mdoc.internal.markdown

import com.vladsch.flexmark.ast.FencedCodeBlock
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import mdoc.internal.cli.Context
import mdoc.internal.markdown.Modifier.Str
import mdoc.internal.markdown.Modifier.Default
import mdoc.internal.markdown.Modifier.Post
import mdoc.internal.markdown.Modifier.Pre

case class PreFenceInput(block: CodeFence, input: Input, mod: Pre)
case class StringFenceInput(block: CodeFence, input: Input, mod: Str)
case class ScalaFenceInput(block: CodeFence, input: Input, mod: Modifier)

class FenceInput(ctx: Context, baseInput: Input) {
  def getModifier(info: Text): Option[Modifier] = {
    val string = info.value.stripLineEnd
    if (!string.startsWith("scala mdoc")) None
    else {
      if (!string.contains(':')) Some(Modifier.Default())
      else {
        val mode = string.stripPrefix("scala mdoc:")
        Modifier(mode)
          .orElse {
            val (name, info) = mode.split(":", 2) match {
              case Array(a) => (a, "")
              case Array(a, b) => (a, b)
            }
            ctx.settings.stringModifiers
              .collectFirst[Modifier] {
                case mod if mod.name == name =>
                  Str(mod, info)
              }
              .orElse {
                ctx.settings.postModifiers.collectFirst {
                  case mod if mod.name == name =>
                    Post(mod, info)
                }
              }
              .orElse {
                ctx.settings.preModifiers.collectFirst {
                  case mod if mod.name == name =>
                    Pre(mod, info)
                }
              }
          }
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

  private def isValid(info: Text, mod: Modifier): Boolean = {
    if (mod.isFailOrWarn && mod.isCrash) {
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
  }
  def unapply(block: CodeFence): Option[ScalaFenceInput] = {
    getModifier(block.info) match {
      case Some(mod) =>
        if (isValid(block.info, mod)) {
          val input = Input.Slice(baseInput, block.body.pos.start, block.body.pos.end)
          Some(ScalaFenceInput(block, input, mod))
        } else {
          None
        }
      case _ => None
    }
  }
}

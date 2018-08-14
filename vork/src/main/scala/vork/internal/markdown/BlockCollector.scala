package vork.internal.markdown

import com.vladsch.flexmark.ast.FencedCodeBlock
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import vork.internal.cli.Context
import vork.internal.markdown.VorkModifier.Custom
import vork.internal.markdown.VorkModifier.Default

case class CustomBlockInput(block: FencedCodeBlock, input: Input, mod: Custom)
case class ScalaBlockInput(block: FencedCodeBlock, input: Input, mod: VorkModifier)

class BlockCollector(ctx: Context, baseInput: Input) {
  def getModifier(block: FencedCodeBlock): Option[VorkModifier] = {
    val string = block.getInfo.toString
    if (!string.startsWith("scala vork")) None
    else {
      if (!string.contains(':')) Some(Default)
      else {
        val mode = string.stripPrefix("scala vork:")
        VorkModifier(mode)
          .orElse {
            val (name, info) = mode.split(":", 2) match {
              case Array(a) => (a, "")
              case Array(a, b) => (a, b)
            }
            ctx.settings.modifiers.collectFirst {
              case mod if mod.name == name =>
                Custom(mod, info)
            }
          }
          .orElse {
            val expected = VorkModifier.all.map(_.toString.toLowerCase()).mkString(", ")
            val msg = s"Invalid mode '$mode'. Expected one of: $expected"
            val offset = "scala vork:".length
            val start = block.getInfo.getStartOffset + offset
            val end = block.getInfo.getEndOffset
            val pos = Position.Range(baseInput, start, end)
            ctx.reporter.error(pos, msg)
            None
          }
      }
    }
  }
  def unapply(block: FencedCodeBlock): Option[ScalaBlockInput] = {
    getModifier(block) match {
      case Some(mod) =>
        val child = block.getFirstChild
        val start = child.getStartOffset
        val end = child.getEndOffset
        val isNewline = baseInput.chars(end - 1) == '\n'
        val cutoff = if (isNewline) 1 else 0
        val input = Input.Slice(baseInput, start, end - cutoff)
        Some(ScalaBlockInput(block, input, mod))
      case _ => None
    }
  }
}

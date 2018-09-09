package mdoc.internal.markdown

import com.vladsch.flexmark.ast.FencedCodeBlock
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import mdoc.internal.cli.Context
import mdoc.internal.markdown.Modifier.Str
import mdoc.internal.markdown.Modifier.Default

case class StringBlockInput(block: FencedCodeBlock, input: Input, mod: Str)
case class ScalaBlockInput(block: FencedCodeBlock, input: Input, mod: Modifier)

class BlockInput(ctx: Context, baseInput: Input) {
  def getModifier(block: FencedCodeBlock): Option[Modifier] = {
    val string = block.getInfo.toString
    if (!string.startsWith("scala mdoc")) None
    else {
      if (!string.contains(':')) Some(Default)
      else {
        val mode = string.stripPrefix("scala mdoc:")
        Modifier(mode)
          .orElse {
            val (name, info) = mode.split(":", 2) match {
              case Array(a) => (a, "")
              case Array(a, b) => (a, b)
            }
            ctx.settings.stringModifiers.collectFirst {
              case mod if mod.name == name =>
                Str(mod, info)
            }
          }
          .orElse {
            val allModifiers =
              Modifier.all.map(_.toString.toLowerCase()) ++
                ctx.settings.stringModifiers.map(_.name)
            val msg = s"Invalid mode '$mode'"
            val offset = "scala mdoc:".length
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

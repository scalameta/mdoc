package mdoc.docs

import com.cibo.evilplot.geometry.Drawable
import java.nio.file.Files
import mdoc._
import scala.meta.inputs.Position

class EvilplotModifier extends PostModifier {
  val name = "evilplot"
  def process(ctx: PostModifierContext): String = {
    val out = ctx.outputFile.resolveSibling(_ => ctx.info)
    ctx.lastValue match {
      case d: Drawable =>
        Files.createDirectories(out.toNIO.getParent)
        if (!out.isFile) {
          d.write(out.toFile)
        }
        s"![](${out.toNIO.getFileName})"
      case _ =>
        val (pos, obtained) = ctx.variables.lastOption match {
          case Some(variable) =>
            val prettyObtained =
              s"${variable.staticType} = ${variable.runtimeValue}"
            (variable.pos, prettyObtained)
          case None =>
            (Position.Range(ctx.originalCode, 0, 0), "nothing")
        }
        ctx.reporter.error(
          pos,
          s"""type mismatch:
  expected: com.cibo.evilplot.geometry.Drawable
  obtained: $obtained"""
        )
        ""
    }
  }
}

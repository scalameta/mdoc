package tests.markdown

import com.cibo.evilplot.geometry.Drawable
import java.nio.file.Files
import mdoc.PostModifier
import mdoc.PostModifierContext

class EvilplotPostModifier extends PostModifier {
  val name = "evilplot"
  var i = 0
  def process(ctx: PostModifierContext): String = {
    val out = ctx.info match {
      case "" => ctx.outputFile.resolveSibling(_ => s"$i.png")
      case filename => ctx.outputFile.resolveSibling(_ => filename)
    }
    ctx.lastValue match {
      case d: Drawable =>
        Files.createDirectories(out.toNIO.getParent)
        if (out.isFile) {
          Files.delete(out.toNIO)
        }
        d.write(out.toFile)
        s"""|
            |![](${out.toNIO.getFileName})
            |""".stripMargin
      case els =>
        ctx.reporter.error(s"expected evilplot drawable. Obtained ${}")
        ""
    }
  }
}

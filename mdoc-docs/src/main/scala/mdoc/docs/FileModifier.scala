package mdoc.docs

import java.nio.charset.StandardCharsets
import mdoc.Reporter
import mdoc.StringModifier
import scala.meta.inputs.Input
import scala.meta.inputs.Position
import scala.meta.internal.io.FileIO
import scala.meta.io.AbsolutePath
import mdoc.internal.pos.PositionSyntax._

class FileModifier extends StringModifier {
  val name = "file"
  override def process(
      info: String,
      code: Input,
      reporter: Reporter
  ): String = {
    val file = AbsolutePath(info)
    if (file.isFile) {
      val text = FileIO.slurp(file, StandardCharsets.UTF_8)
      s"""
File: [${file.toNIO.getFileName}](https://github.com/scalameta/mdoc/blob/master/$info)
`````scala
$text
`````
"""
    } else {
      val pos = Position.Range(code, 0 - info.length - 1, 0 - 1).toUnslicedPosition
      reporter.error(pos, s"no such file: $file")
      ""
    }
  }

}

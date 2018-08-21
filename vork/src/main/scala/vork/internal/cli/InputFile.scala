package mdoc.internal.cli

import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath

case class InputFile(
    relpath: RelativePath,
    in: AbsolutePath,
    out: AbsolutePath
)

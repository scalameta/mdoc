package mdoc.internal.markdown

import scala.meta.inputs.Position

final case class Rename(from: Position, to: String)

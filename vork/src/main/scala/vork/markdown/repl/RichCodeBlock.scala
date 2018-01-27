package vork.markdown.repl

import java.nio.file.Path

case class Position(path: Path, startLine: Int, endLine: Int) {
  override def toString: String = s"${path.toAbsolutePath}:$startLine:$endLine"
}

case class RichCodeBlock(code: String, pos: Position)

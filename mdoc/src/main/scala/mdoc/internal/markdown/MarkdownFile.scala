package mdoc.internal.markdown

import scala.meta.inputs.Position
import scala.meta.inputs.Input

import scala.collection.mutable
import scala.meta.io.RelativePath

import mdoc.internal.cli.InputFile
import mdoc.parser._

final case class MarkdownFile(input: Input, file: InputFile, parts: List[MarkdownPart]) {
  private val appends = mutable.ListBuffer.empty[String]
  def appendText(text: String): Unit = {
    appends += text
  }
  def renderToString: String = {
    val out = new StringBuilder()
    parts.foreach(_.renderToString(out))
    appends.foreach(a => out.append(a).append("\n"))
    out.toString()
  }
}
object MarkdownFile {
  def parse(
      input: Input,
      file: InputFile,
      settings: ParserSettings
  ): MarkdownFile = {
    val parts = MarkdownPart.parse(input.text, settings)
    MarkdownFile(input, file, parts)
  }
}

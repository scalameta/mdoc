package mdoc

import scala.meta.inputs.Input
import scala.meta.io.RelativePath
import mdoc.internal.cli.Settings
import mdoc.internal.cli.CliEnrichments._
import scala.meta.io.AbsolutePath
import mdoc.internal.cli.InputFile
import mdoc.parser.Text

final class PreModifierContext private[mdoc] (
    val info: String,
    val fences: Text,
    val originalCode: Input,
    val reporter: Reporter,
    private[mdoc] val file: InputFile,
    private[mdoc] val settings: Settings
) {
  def infoInput: Input = {
    val cpos = originalCode.toPosition.toUnslicedPosition
    val start = cpos.start - info.length - 1
    val end = cpos.start - 1
    Input.Slice(cpos.input, start, end)
  }
  def relativePath: RelativePath = file.relpath
  def inputFile: AbsolutePath = file.inputFile
  def outputFile: AbsolutePath = file.outputFile
  def inDirectory: AbsolutePath = file.inputDirectory
  def outDirectory: AbsolutePath = file.outputDirectory
}

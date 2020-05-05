package mdoc

import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import mdoc.internal.cli.Settings
import mdoc.internal.cli.InputFile

final class PostProcessContext private[mdoc] (
    val reporter: Reporter,
    private[mdoc] val file: InputFile,
    private[mdoc] val settings: Settings
) {
  def relativePath: RelativePath = file.relpath
  def inputFile: AbsolutePath = file.inputFile
  def outputFile: AbsolutePath = file.outputFile
  def inDirectory: AbsolutePath = file.inputDirectory
  def outDirectory: AbsolutePath = file.outputDirectory
}

package mdoc.internal.cli

import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import scala.meta.internal.io.PathIO
import metaconfig.Input
import java.nio.file.Files
import mdoc.internal.pos.PositionSyntax._

/** @param relpath the input filename relativized by its input directory.
  * @param inputFile the input file to read from.
  * @param outputFile the output file to write to.
  * @param inputDirectory directory enclosing the input file.
  * @param outputDirectory directory enclosing the output file.
  */
case class InputFile(
    relpath: RelativePath,
    inputFile: AbsolutePath,
    outputFile: AbsolutePath,
    inputDirectory: AbsolutePath,
    outputDirectory: AbsolutePath
)

object InputFile {
  implicit val ordering: Ordering[InputFile] = new Ordering[InputFile] {
    def compare(x: InputFile, y: InputFile): Int = {
      x.inputFile.toNIO.compareTo(y.inputFile.toNIO)
    }
  }

  def fromRelativeFilename(filename: String, settings: Settings): InputFile = {
    val relpath = RelativePath(filename)
    val inputDir = settings.in.head
    val outputDir = settings.out.head
    InputFile(
      relpath,
      inputDir.resolve(filename),
      outputDir.resolve(filename),
      inputDir,
      outputDir
    )
  }
}

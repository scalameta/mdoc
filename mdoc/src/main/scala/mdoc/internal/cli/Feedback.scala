package mdoc.internal.cli

import java.nio.file.Path
import scala.meta.io.AbsolutePath

object Feedback {
  def outSubdirectoryOfIn(in: Path, out: Path): String = {
    s"--out cannot be a subdirectory of --in because otherwise mdoc would need to process its own generated docs. " +
      "To fix this problem, change --out so that it points to an independent directory from --in.\n" +
      s"  --in=$in\n" +
      s"  --out=$out"
  }
  def mustBeNonEmpty(what: String): String = {
    s"--$what must be non-empty. To fix this problem, add a value for the `--$what <value>` argument."
  }
  def inputDifferentLengthOutput(input: List[AbsolutePath], output: List[AbsolutePath]): String = {
    val diff = math.abs(input.length - output.length)
    val toFix = if (input.length > output.length) "out" else "in"
    s"--in and --out must have the same length but found ${input.length} --in argument(s) and ${output.length} --out argument(s). " +
      s"To fix this problem, add $diff more $toFix arguments."
  }
  def outputCannotBeRegularFile(input: AbsolutePath, output: AbsolutePath): String = {
    s"--out argument '$output' cannot be a regular file when --in argument '$input' is a directory."
  }
  def outputCannotBeDirectory(input: AbsolutePath, output: AbsolutePath): String = {
    s"--out argument '$output' cannot be a directory when --in argument '$input' is a regular file. " +
      "To fix this problem, change the --out argument to point to a regular file or an empty path."
  }
  def inputEqualOutput(input: AbsolutePath): String = {
    s"--in and --out cannot be the same path '$input'. " +
      "To fix this problem, change the --out argument to another path."
  }
}

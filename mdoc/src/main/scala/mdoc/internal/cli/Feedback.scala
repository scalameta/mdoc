package mdoc.internal.cli

import java.nio.file.Path

object Feedback {
  def outSubdirectoryOfIn(in: Path, out: Path): String = {
    s"--out cannot be a subdirectory of --in because otherwise mdoc would need to process its own generated docs. " +
      "To fix this problem, change --out so that it points to an independent directory from --in.\n" +
      s"  --in=$in\n" +
      s"  --out=$out"
  }
}

package mdoc.docs

import java.nio.file.FileSystems
import java.nio.file.Paths
import scala.meta.internal.io.PathIO
import mdoc.Main
import mdoc.MainSettings
import mdoc.StringModifier
import mdoc.internal.BuildInfo
import mdoc.modifiers.ScastieModifier

object Docs {
  def stableVersion: String =
    BuildInfo.version.replaceFirst("\\+.*", "")
  def main(args: Array[String]): Unit = {
    val cwd = PathIO.workingDirectory.toNIO
    val fs = FileSystems.getDefault
    val out = cwd.resolve("website").resolve("target").resolve("docs")
    val settings = MainSettings()
      .withOut(out)
      .withExcludePath(
        List(
          fs.getPathMatcher("glob:vscode-extension")
        )
      )
      .withCleanTarget(false)
      .withReportRelativePaths(true)
      .withStringModifiers(
        StringModifier.default() ++ List(
          new ScastieModifier(debugClassSuffix = Some("<a_random_uuid>"))
        )
      )
      .withArgs(args.toList)
    val context = settings.settings.validate(settings.reporter).get
    val exitCode = Main.process(
      settings
        .withStringModifiers(
          StringModifier.default() ++ List(
            new MdocModifier(context)
          )
        )
    )
    if (exitCode != 0) sys.exit(exitCode)
  }
}

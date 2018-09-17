package mdoc.docs

import java.nio.file.Paths
import scala.meta.internal.io.PathIO
import mdoc.Main
import mdoc.MainSettings
import mdoc.internal.BuildInfo
import mdoc.modifiers.ScastieModifier

object Docs {
  def main(args: Array[String]): Unit = {
    val cwd = PathIO.workingDirectory.toNIO
    val settings = MainSettings()
      .withIn(Paths.get("docs"))
      .withOut(cwd)
      .withSiteVariables(Map("VERSION" -> BuildInfo.stableVersion))
      .withCleanTarget(false)
      .withReportRelativePaths(true)
      .withStringModifiers(
        List(
          new FooModifier,
          new ScastieModifier(debugClassSuffix = Some("<a_random_uuid>"))
        )
      )
      .withArgs(args.toList)
    val context = settings.settings.validate(settings.reporter).get
    val exitCode = Main.process(
      settings
        .withStringModifiers(
          List(
            new FooModifier,
            new MdocModifier(context)
          )
        )
    )
    if (exitCode != 0) sys.exit(exitCode)
  }
}

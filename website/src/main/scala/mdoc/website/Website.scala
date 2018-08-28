package mdoc.website

import java.nio.file.Paths
import scala.meta.internal.io.PathIO
import mdoc.Main
import mdoc.MainSettings
import mdoc.internal.BuildInfo
import mdoc.modifiers.ScastieModifier

object Website {
  def main(args: Array[String]): Unit = {
    val cwd = PathIO.workingDirectory.toNIO
    val settings = MainSettings()
      .withIn(Paths.get("docs"))
      .withOut(cwd)
      .withSiteVariables(Map("VERSION" -> BuildInfo.stableVersion))
      .withCleanTarget(false)
      .withArgs(args.toList)
      .withReportRelativePaths(true)
      .withWatch(true)
      .withStringModifiers(
        List(
          new FooModifier,
          new ScastieModifier(debugClassSuffix = Some("<a_random_uuid>"))
        )
      )
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
    sys.exit(exitCode)
  }
}

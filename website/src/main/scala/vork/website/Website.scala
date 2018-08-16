package vork.website

import java.nio.file.Paths
import scala.meta.internal.io.PathIO
import vork.Main
import vork.MainSettings
import vork.internal.BuildInfo

object Website {
  def main(args: Array[String]): Unit = {
    val cwd = PathIO.workingDirectory.toNIO
    val settings = MainSettings()
      .withIn(Paths.get("docs"))
      .withOut(cwd)
      .withSiteVariables(Map("VERSION" -> BuildInfo.version))
      .withCleanTarget(false)
      .withTest(args.toList == List("test"))
    val context = settings.settings.validate(settings.reporter).get
    val stringModifier = new VorkStringModifier(context)
    val exitCode = Main.process(settings.withStringModifiers(List(stringModifier)))
    sys.exit(exitCode)
  }
}

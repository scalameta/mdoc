package mdoc.docs

import java.nio.file.FileSystems
import scala.meta.internal.io.PathIO
import mdoc.Main
import mdoc.MainSettings
import mdoc.StringModifier
import mdoc.internal.BuildInfo
import mdoc.modifiers.ScastieModifier
import mdoc.internal.cli.Context

object Docs {
  def stableVersion: String =
    BuildInfo.version.replaceFirst("\\+.*", "")
  def main(_args: Array[String]): Unit = {
    val cwd = PathIO.workingDirectory.toNIO
    val fs = FileSystems.getDefault
    val isBlog = _args.headOption.contains("blog")
    val args =
      if (isBlog) _args.toList.tail
      else _args.toList
    val blogIn = cwd.resolve("blog")
    val blogOut = cwd.resolve("website").resolve("blog")
    val in =
      if (isBlog) blogIn
      else cwd.resolve("docs")
    val out =
      if (isBlog) blogOut
      else cwd.resolve("website").resolve("target").resolve("docs")
    val base = MainSettings()
    val settings = base
      .withIn(in)
      .withOut(out)
      .withExcludePath(
        List(
          fs.getPathMatcher("glob:vscode-extension")
        )
      )
      .withSiteVariables(
        base.settings.site ++ Map(
          "js-relative-link-prefix" -> {
            if (isBlog) "../../../" // hop over 2019/01/04 URL
            else ""
          }
        )
      )
      .withCleanTarget(false)
      .withReportRelativePaths(true)
      .withStringModifiers(
        StringModifier.default() ++ List(
          new ScastieModifier(debugClassSuffix = Some("<a_random_uuid>"))
        )
      )
      .withArgs(args)
    val context = Context.fromSettings(settings.settings, settings.reporter).get

    val exitCode = Main.process(
      settings
        .withStringModifiers(
          StringModifier.default() ++ List(
            new MdocModifier(context)
          )
        )
    )
    if (exitCode != 0) sys.exit(exitCode)
    if (_args.isEmpty) {
      val blogExit = Main.process(
        settings
          .withOut(blogOut)
          .withIn(blogIn)
      )
      if (blogExit != 0) sys.exit(exitCode)
    }
  }
}

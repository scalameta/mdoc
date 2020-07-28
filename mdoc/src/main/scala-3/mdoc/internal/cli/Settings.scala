package mdoc.internal.cli

import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.StandardCharsets
import java.nio.charset.UnsupportedCharsetException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.PathMatcher
import mdoc.OnLoadContext
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.ConfEncoder
import metaconfig.ConfError
import metaconfig.Configured
import metaconfig.annotation._
import metaconfig.generic
import metaconfig.generic.Surface
import org.typelevel.paiges.Doc
import scala.annotation.StaticAnnotation
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.meta.io.RelativePath
import mdoc.Reporter
import mdoc.internal.markdown.MarkdownCompiler

class Section(val name: String) extends StaticAnnotation

// This class avoids having to upgrade metaconfig with support for Dotty macros
case class Settings(
    @Section("Common options")
    @Description(
      "The input directory or regular file containing markdown and other documentation sources that should be processed. " +
        "Markdown files are processed by mdoc while non-markdown files are copied verbatim to the output directory. " +
        "Can be repeated to process multiple input directories/files."
    )
    @ExtraName("i")
    in: List[AbsolutePath],
    @Description(
      "The output directory or regular file where you'd like to generate your markdown or other documentation sources. " +
        "Must be repeated to match the number of `--in` arguments and must be a directory when the matching `--in` argument is a directory."
    )
    @ExtraName("o")
    out: List[AbsolutePath],
    @Description(
      "Key/value pairs of variables to replace through @VAR@. " +
        "For example, the flag '--site.VERSION 1.0.0' will replace appearances of '@VERSION@' in " +
        "markdown files with the string 1.0.0"
    )
    site: Map[String, String] = Map.empty,
    @Section("Compiler options")
    @Description(
      "Classpath to use when compiling Scala code examples. " +
        "Defaults to the current thread's classpath." +
        "You can use Coursier's fetch command to generate the classpath for you:" +
        "`--classpath $(cs fetch --classpath org::artifact:version)`"
    )
    classpath: String = "",
    @Description(
      "Space separated list of compiler flags such as '-Xplugin:kind-projector.jar -deprecation -Yrangepos'. " +
        "Defaults to the value of 'scalacOptions' in the 'mdoc.properties' resource file, if any. " +
        "When using sbt-mdoc, update the `scalacOptions` sbt setting instead of passing --scalac-options to `mdocExtraArguments`."
    )
    scalacOptions: String = "",
    @Description(
      "Use relative filenames when reporting error messages. " +
        "Useful for producing consistent docs on a local machine and CI. "
    )
    reportRelativePaths: Boolean = false,
    @Description("The encoding to use when reading and writing files.")
    charset: Charset = StandardCharsets.UTF_8,
    @Description("The working directory to use for making relative paths absolute.")
    cwd: AbsolutePath,
    @Hidden()
    @Description("The width of the screen, used to line wrap pretty-printed objects.")
    screenWidth: Int = 120,
    @Hidden()
    @Description("The height of the screen, used to truncate large pretty-printed objects.")
    screenHeight: Int = 50,
    @Hidden()
    @Description("The Coursier logger used to report progress bars when downloading dependencies")
    coursierLogger: coursierapi.Logger = coursierapi.Logger.progressBars()
) {

  def withProperties(props: MdocProperties): Settings =
    copy(
      scalacOptions = props.scalacOptions,
      classpath = props.classpath,
      site = site ++ props.site,
      in = props.in.getOrElse(in),
      out = props.out.getOrElse(out)
    )

  def withWorkingDirectory(dir: AbsolutePath): Settings = {
    copy(
      in = List(dir.resolve("docs")),
      out = List(dir.resolve("out")),
      cwd = dir
    )
  }
}

object Settings {
  def baseDefault(cwd: AbsolutePath): Settings = {
    new Settings(
      in = List(cwd.resolve("docs")),
      out = List(cwd.resolve("out")),
      cwd = cwd
    )
  }
  def default(cwd: AbsolutePath): Settings = {
    val base = baseDefault(cwd)
    val props = MdocProperties.default(cwd)
    base.withProperties(props)
  }

  // note(@tgodzik) not used currently for Scala 3
  def fromCliArgs(args: List[String], base: Settings): Configured[Settings] = Configured.ok(base)

}

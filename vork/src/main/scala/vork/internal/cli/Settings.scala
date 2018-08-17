package vork.internal.cli

import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.StandardCharsets
import java.nio.charset.UnsupportedCharsetException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.PathMatcher
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
import vork.StringModifier
import vork.Reporter
import vork.internal.BuildInfo
import vork.internal.markdown.MarkdownCompiler

class Section(val name: String) extends StaticAnnotation

case class Settings(
    @Section("Common options")
    @Description(
      "The input directory containing markdown and other documentation sources. " +
        "Markdown files will be processed by vork while other files will be copied " +
        "verbatim to the output directory."
    )
    @ExtraName("i")
    in: AbsolutePath,
    @Description("The output directory to generate the vork site.")
    @ExtraName("o")
    out: AbsolutePath,
    @Description("Start a file watcher and incrementally re-generate the site on file save.")
    @ExtraName("w")
    watch: Boolean = false,
    @Description(
      "Instead of generating a new site, report an error if generating the site would produce a diff " +
        "against an existing site. Useful for asserting in CI that a site is up-to-date."
    )
    test: Boolean = false,
    @Description(
      "Classpath to use when compiling Scala code examples. " +
        "Defaults to the current thread's classpath."
    )
    classpath: String = "",
    @Description(
      "Key/value pairs of variables to replace through @VAR@. " +
        "For example, the flag '--site.VERSION 1.0.0' will replace appearances of '@VERSION@' in " +
        "markdown files with the string 1.0.0"
    )
    site: Map[String, String] = Map.empty,
    @Description("Remove all files in the outout directory before generating a new site.")
    cleanTarget: Boolean = false,
    @Section("Less common options")
    @Description("Print out a help message and exit")
    help: Boolean = false,
    @Description("Print out usage instructions and exit")
    usage: Boolean = false,
    @Description("Print out the version number and exit")
    version: Boolean = false,
    @Description("Glob to filter which files from --in directory to include.")
    includePath: List[PathMatcher] = Nil,
    @Description("Glob to filter which files from --in directory to exclude.")
    excludePath: List[PathMatcher] = Nil,
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
    stringModifiers: List[StringModifier] = Nil
) {

  def toInputFile(infile: AbsolutePath): Option[InputFile] = {
    val relpath = infile.toRelative(in)
    if (matches(relpath)) {
      val outfile = out.resolve(relpath)
      Some(InputFile(relpath, infile, outfile))
    } else {
      None
    }
  }
  def matches(path: RelativePath): Boolean = {
    (includePath.isEmpty || includePath.exists(_.matches(path.toNIO))) &&
    !excludePath.exists(_.matches(path.toNIO))
  }
  def validate(logger: Reporter): Configured[Context] = {
    if (Files.exists(in.toNIO)) {
      val compiler = MarkdownCompiler.fromClasspath(classpath)
      Configured.ok(Context(this, logger, compiler))
    } else {
      ConfError.fileDoesNotExist(in.toNIO).notOk
    }
  }
  def resolveIn(relpath: RelativePath): AbsolutePath = {
    in.resolve(relpath)
  }

  def resolveOut(relpath: RelativePath): AbsolutePath = {
    out.resolve(relpath)
  }
}

object Settings extends MetaconfigScalametaImplicits {
  def default(cwd: AbsolutePath): Settings = new Settings(
    in = cwd.resolve("docs"),
    out = cwd.resolve("out"),
    cwd = cwd
  )
  def fromCliArgs(args: List[String], base: Settings): Configured[Settings] = {
    Conf
      .parseCliArgs[Settings](args)
      .andThen(_.as[Settings](decoder(base)))
  }
  def version =
    s"Vork v${BuildInfo.version}"
  def usage: String =
    """|Usage:   vork [<option> ...]
       |Example: vork --in <path> --out <path> (customize input/output directories)
       |         vork --watch                  (watch for file changes)
       |         vork --site.VERSION 1.0.0     (pass in site variables)
       |         vork --exclude-path <glob>    (exclude files matching patterns)
       |""".stripMargin
  def description: Doc =
    Doc.paragraph(
      """|Vork is a documentation tool that interprets Scala code examples within markdown
         |code fences allowing you to compile and test documentation as part your build.
         |""".stripMargin
    )
  def help: HelpMessage[Settings] =
    new HelpMessage[Settings](default(PathIO.workingDirectory), version, usage, description)
  implicit val surface: Surface[Settings] =
    generic.deriveSurface[Settings]
  implicit val encoder: ConfEncoder[Settings] =
    generic.deriveEncoder[Settings]
  def decoder(base: Settings): ConfDecoder[Settings] = {
    implicit val cwd = base.cwd
    implicit val PathDecoder: ConfDecoder[AbsolutePath] =
      ConfDecoder.stringConfDecoder.flatMap { str =>
        try {
          Configured.ok(AbsolutePath(str))
        } catch {
          case e: InvalidPathException =>
            ConfError.message(e.getMessage).notOk
        }
      }
    generic.deriveDecoder[Settings](base)
  }

  implicit val pathMatcherDecoder: ConfDecoder[PathMatcher] =
    ConfDecoder.stringConfDecoder.map(glob => FileSystems.getDefault.getPathMatcher("glob:" + glob))
  implicit val CharsetDecoder: ConfDecoder[Charset] =
    ConfDecoder.stringConfDecoder.flatMap { str =>
      try {
        Configured.ok(Charset.forName(str))
      } catch {
        case _: UnsupportedCharsetException =>
          ConfError.message(s"Charset '$str' is unsupported").notOk
        case _: IllegalCharsetNameException =>
          ConfError.message(s"Charset name '$str' is illegal").notOk
      }
    }

  implicit val pathEncoder: ConfEncoder[AbsolutePath] =
    ConfEncoder.StringEncoder.contramap(_.toString())
  implicit val pathMatcherEncoder: ConfEncoder[PathMatcher] =
    ConfEncoder.StringEncoder.contramap(_.toString())
  implicit val charsetEncoder: ConfEncoder[Charset] =
    ConfEncoder.StringEncoder.contramap(_.name())

}

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
import mdoc.PostModifier
import mdoc.PreModifier
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
import mdoc.StringModifier
import mdoc.Variable
import mdoc.Reporter
import mdoc.internal.markdown.{GitHubIdGenerator, ReplVariablePrinter}
import mdoc.internal.cli.CliEnrichments._
import pprint.TPrint
import pprint.TPrintColors

class Section(val name: String) extends StaticAnnotation

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
    @Description("Start a file watcher and incrementally re-generate the site on file save.")
    @ExtraName("w")
    watch: Boolean = false,
    @Description(
      "Instead of generating a new site, report an error if generating the site would produce a diff " +
        "against an existing site. Useful for asserting in CI that a site is up-to-date."
    )
    @ExtraName("test")
    check: Boolean = false,
    @Description("Disable link hygiene analysis so that no warnings are reported for dead links.")
    noLinkHygiene: Boolean = false,
    @Description(
      "Link hygiene analysis reports dead links as errors. Useful for asserting in CI that the site contains no dead links. This setting has no effect in watch mode. It is also mutually exclusive with --no-link-hygiene, if both are set, link hygiene is disabled."
    )
    checkLinkHygiene: Boolean = false,
    @Description("Include additional diagnostics for debugging potential problems.")
    verbose: Boolean = false,
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
    @Description("Remove all files in the output directory before generating a new site.")
    cleanTarget: Boolean = false,
    @Section("LiveReload options")
    @Description("Don't start a LiveReload server under --watch mode.")
    noLivereload: Boolean = false,
    @Description(
      "Which port the LiveReload server should listen to. " +
        "If the port is not free, another free port close to this number is used."
    )
    port: Int = 4000,
    @Description("Which hostname the LiveReload server should listen to")
    host: String = "localhost",
    @Section("Less common options")
    @Description("Print out a help message and exit")
    help: Boolean = false,
    @Description("Print out usage instructions and exit")
    usage: Boolean = false,
    @Description("Print out the version number and exit")
    version: Boolean = false,
    @Description("Set of file extensions to treat as markdown files.")
    markdownExtensions: List[String] = List("md", "html"),
    @Description(
      "Glob to filter which files to process. Defaults to all files. " +
        "Example: --include **/example.md will process only files with the name example.md."
    )
    @ExtraName("includePath")
    include: List[PathMatcher] = Nil,
    @Description(
      "Glob to filter which files from exclude from processing. Defaults to no files. " +
        "Example: --include users/**.md --exclude **/example.md will process all files in the users/ directory " +
        "excluding files named example.md."
    )
    @ExtraName("excludePath")
    exclude: List[PathMatcher] = Nil,
    @Description(
      "Use relative filenames when reporting error messages. " +
        "Useful for producing consistent docs on a local machine and CI. "
    )
    reportRelativePaths: Boolean = false,
    @Description("The encoding to use when reading and writing files.")
    charset: Charset = StandardCharsets.UTF_8,
    @Description("The working directory to use for making relative paths absolute.")
    cwd: AbsolutePath,
    @Description("Allow indented code fence blocks")
    allowCodeFenceIndented: Boolean = false,
    @Hidden()
    stringModifiers: List[StringModifier] = StringModifier.default(),
    @Hidden()
    postModifiers: List[PostModifier] = PostModifier.default(),
    @Hidden()
    preModifiers: List[PreModifier] = PreModifier.default(),
    @Hidden()
    @Description("The input stream to listen for enter key during file watching.")
    inputStream: InputStream = System.in,
    @Hidden()
    @Description("The width of the screen, used to line wrap pretty-printed objects.")
    screenWidth: Int = 120,
    @Hidden()
    @Description("The height of the screen, used to truncate large pretty-printed objects.")
    screenHeight: Int = 50,
    @Hidden()
    @Description("The generator for header IDs, defaults to GitHub ID generator")
    headerIdGenerator: String => String = GitHubIdGenerator,
    @Hidden()
    @Description("The pretty printer for variables")
    variablePrinter: Variable => String = ReplVariablePrinter,
    @Hidden()
    @Description("The Coursier logger used to report progress bars when downloading dependencies")
    coursierLogger: coursierapi.Logger = coursierapi.Logger.progressBars()
) extends mdoc.parser.ParserSettings {

  val isMarkdownFileExtension = markdownExtensions.toSet

  val outputByInput = in
    .zip(out)
    .iterator
    .map { case (input, output) =>
      // NOTE(olafur): we automatically infer a filename for --out when --out
      // is a directory and --in points to a regular file. For example, the
      // command `mdoc --in readme.md` writes the output to the path
      // `out/readme.md` even if the default value for --out is `out/`
      // (without `readme.md`).
      if (input.isFile && output.isDirectory) input -> output.resolve(input.filename)
      else input -> output
    }
    .toMap

  def withProperties(props: MdocProperties): Settings =
    copy(
      scalacOptions = props.scalacOptions,
      classpath = props.classpath,
      site = site ++ props.site,
      in = props.in.getOrElse(in),
      out = props.out.getOrElse(out)
    )

  override def toString: String = Settings.write(this).toString()

  def isFileWatching: Boolean = watch && !check

  def toInputFile(infile: AbsolutePath): Option[InputFile] = {
    outputByInput.get(infile) match {
      case Some(outfile) =>
        Some(
          InputFile(
            RelativePath(infile.toNIO.getFileName()),
            infile,
            outfile,
            infile.parent,
            outfile.parent
          )
        )
      case None =>
        outputByInput.collectFirst {
          case (inputDir, outputDir) if infile.toNIO.startsWith(inputDir.toNIO) =>
            val relpath = infile.toRelative(inputDir)
            InputFile(relpath, infile, outputDir.resolve(relpath), inputDir, outputDir)
        }
    }
  }

  def isExplicitlyExcluded(path: RelativePath): Boolean = {
    exclude.exists(_.matches(path.toNIO))
  }

  def isIncluded(path: RelativePath): Boolean = {
    (include.isEmpty || include.exists(_.matches(path.toNIO))) &&
    !isExplicitlyExcluded(path)
  }

  def onLoad(reporter: Reporter): Unit = {
    val ctx = new OnLoadContext(reporter, this)
    preModifiers.foreach(_.onLoad(ctx))
  }

  def addSite(extraVariables: Map[String, String]): Settings = {
    copy(site = extraVariables ++ site)
  }
  def withWorkingDirectory(dir: AbsolutePath): Settings = {
    copy(
      in = List(dir.resolve("docs")),
      out = List(dir.resolve("out")),
      cwd = dir
    )
  }
}

object Settings { // extends MetaconfigScalametaImplicits with Decoders with SettingsGeneric {
  def version(displayVersion: String) =
    s"mdoc v$displayVersion"
  def usage: String =
    """|Usage:   mdoc [<option> ...]
       |Example: mdoc --in mydocs --out _site  (custom input/output directories)
       |         mdoc --watch                  (watch for file changes)
       |         mdoc --site.VERSION 1.0.0     (pass in site variables)
       |         mdoc --include **/example.md  (process only files named example.md)
       |         mdoc --exclude node_modules   (don't process node_modules directory)
       |         mdoc --in readme.template.md --out readme.md \
       |              --in examples.template.md --out examples.md (multiple input/output pairs)
       |""".stripMargin
  def description: Doc =
    Doc.paragraph(
      """|mdoc is a documentation tool that interprets Scala code examples within markdown
         |code fences allowing you to compile and test documentation as part your build.
         |""".stripMargin
    )

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

  def help(displayVersion: String, width: Int): String =
    new HelpMessage[Settings](
      baseDefault(PathIO.workingDirectory),
      version(displayVersion),
      usage,
      description
    ).helpMessage(width)

  implicit def surface: Surface[Settings] =
    generic.deriveSurface[Settings]
  implicit def encoder: ConfEncoder[Settings] =
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

  def fromCliArgs(args: List[String], base: Settings): Configured[Settings] = {
    Conf
      .parseCliArgs[Settings](args)
      .andThen(conf => {
        val cwd = conf.get[String]("cwd").map(AbsolutePath(_)(base.cwd)).getOrElse(base.cwd)
        conf.as[Settings](decoder(base.copy(cwd = cwd)))
      })
      .map(_.addSite(base.site))
  }

  def write(set: Settings) = ConfEncoder[Settings].write(set)

  implicit val absolutePathPrint: TPrint[AbsolutePath] =
    new TPrint[AbsolutePath] {
      def render(implicit tpc: TPrintColors): fansi.Str = fansi.Str("<path>")
    }

  implicit val pathMatcherPrint: TPrint[PathMatcher] =
    new TPrint[PathMatcher] {
      def render(implicit tpc: TPrintColors): fansi.Str = fansi.Str("<glob>")
    }

  implicit def optionPrint[T](implicit ev: pprint.TPrint[T]): TPrint[Option[T]] =
    new TPrint[Option[T]] {
      def render(implicit tpc: TPrintColors): fansi.Str = ev.render
    }

  implicit def iterablePrint[C[x] <: Iterable[x], T](implicit ev: pprint.TPrint[T]): TPrint[C[T]] =
    new TPrint[C[T]] {
      def render(implicit tpc: TPrintColors): fansi.Str = s"[${ev.render} ...]"
    }

  implicit val pathMatcherDecoder: ConfDecoder[PathMatcher] =
    ConfDecoder.stringConfDecoder.map(glob => FileSystems.getDefault.getPathMatcher("glob:" + glob))
  implicit val LoggerEncoder: ConfEncoder[coursierapi.Logger] =
    ConfEncoder.StringEncoder.contramap(_.toString())
  implicit val LoggerDecoder: ConfDecoder[coursierapi.Logger] =
    ConfDecoder.stringConfDecoder.flatMap {
      case "nop" => Configured.ok(coursierapi.Logger.nop())
      case "progress-bars" => Configured.ok(coursierapi.Logger.progressBars())
      case unknown =>
        Configured.error(
          s"unknown coursier logger '$unknown'. Expected one of 'nop' or 'progress-bars'"
        )
    }
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
  implicit val inputStreamDecoder: ConfDecoder[InputStream] =
    ConfDecoder.stringConfDecoder.map(_ => System.in)
  implicit val headerIdGeneratorDecoder: ConfDecoder[String => String] =
    ConfDecoder.stringConfDecoder.flatMap(_ => ConfError.message("unsupported").notOk)
  implicit val variablePrinterDecoder: ConfDecoder[Variable => String] =
    ConfDecoder.stringConfDecoder.flatMap(_ => ConfError.message("unsupported").notOk)

  implicit val pathEncoder: ConfEncoder[AbsolutePath] =
    ConfEncoder.StringEncoder.contramap { path =>
      if (path == PathIO.workingDirectory) "<current working directory>"
      else path.toRelative(PathIO.workingDirectory).toString()
    }
  implicit val pathMatcherEncoder: ConfEncoder[PathMatcher] =
    ConfEncoder.StringEncoder.contramap(_.toString())
  implicit val charsetEncoder: ConfEncoder[Charset] =
    ConfEncoder.StringEncoder.contramap(_.name())
  implicit val inputStreamEncoder: ConfEncoder[InputStream] =
    ConfEncoder.StringEncoder.contramap(_ => "<input stream>")
  implicit val headerIdGeneratorEncoder: ConfEncoder[String => String] =
    ConfEncoder.StringEncoder.contramap(_ => "<String => String>")
  implicit val variablePrinterEncoder: ConfEncoder[Variable => String] =
    ConfEncoder.StringEncoder.contramap(_ => "<Variable => String>")
}

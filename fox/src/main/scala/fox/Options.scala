package fox

import java.io.File
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.nio.file.Path
import java.nio.file.Paths
import caseapp.AppName
import caseapp.AppVersion
import caseapp.ExtraName
import caseapp.HelpMessage
import caseapp.ProgName
import fox.Enrichments._
import fox.Markdown.Doc
import fox.Markdown.Header
import fox.Markdown.Site

object Enrichments {
  implicit class XtensionPathUrl(val path: Path) extends AnyVal {
    def stripSuffix(suffix: String): Path =
      path.resolveSibling(path.getFileName.toString.stripSuffix(suffix))
  }
}

@AppName("fox")
@AppVersion("0.1.0-SNAPSHOT")
@ProgName("fox")
case class Options(
    @HelpMessage("The input directory to generate the fox site.")
    @ExtraName("i")
    in: String = Paths.get("docs").toString,
    @HelpMessage("The output directory to generate the fox site.")
    @ExtraName("o")
    out: String = Paths.get("target").resolve("fox").toString,
    cwd: String = Paths.get(".").toAbsolutePath.toString,
    repoName: String = "olafurpg/fox",
    repoUrl: String = "https://github.com/olafurpg/fox",
    title: String = "Fox",
    description: String = "My Description",
    googleAnalytics: Option[String] = None,
    copyright: String =
      """Inspired by <a href='https://jonas.github.io/paradox-material-theme/'>paradox-material-theme</a>
        |and <a href='http://squidfunk.github.io/mkdocs-material/'>mkdocs-material</a>
        |""".stripMargin,
    classpath: List[String] = Options.defaultClasspath,
    cleanTarget: Boolean = false,
    baseUrl: String = "",
    encoding: String = "UTF-8",
    configPath: String = Paths.get("fox.conf").toString
) {

  private final val indexMd = Paths.get("index.md")
  lazy val config: Config = Config.from(Paths.get(configPath))

  def asset(path: String) = s"$baseUrl/assets/$path"
  def lib(path: String) = s"$baseUrl/lib/$path"
  def hrefGithub(doc: Doc) = s"$repoUrl/tree/master/$in/${doc.path}"

  def href(doc: Doc, withBase: Boolean = true): String = {
    val base = if (withBase) baseUrl else ""
    if (doc.path == indexMd) s"$base/"
    else if (doc.path.endsWith(indexMd)) {
      s"$base/${doc.path.getParent.toString}"
    } else {
      s"$base/${doc.path.toString}".stripSuffix(".md")
    }
  }

  def charset: Charset = Charset.forName(encoding)
  def resolveIn(relpath: Path): Path = {
    require(!relpath.isAbsolute)
    inPath.resolve(relpath)
  }

  // We may want to change the way the layout is resolved.
  def resolveOut(relpath: Path): Path = {
    require(!relpath.isAbsolute)
    val base = outPath.resolve(relpath)
    if (relpath.endsWith(indexMd)) {
      // foo/specimen.md => foo/index.html
      base.resolveSibling(indexMd)
    } else {
      // foo/bar/something.md => foo/bar/index.html
      outPath.resolve(relpath).stripSuffix(".md").resolve(indexMd)
    }
  }

  def prettyPath(path: Path): String = cwdPath.relativize(path).toString
  lazy val cwdPath: Path = Paths.get(cwd).toAbsolutePath.normalize()
  lazy val inPath: Path = Paths.get(in).toAbsolutePath.normalize()
  lazy val outPath: Path = Paths.get(out).toAbsolutePath.normalize()
  lazy val searchIndexPath: Path =
    outPath.resolve("mkdocs").resolve("search_index.json")
  def classpathOpts: List[String] =
    "-classpath" :: classpath.mkString(File.pathSeparator) :: Nil
}

object Options {
  def defaultClasspath: List[String] = this.getClass.getClassLoader match {
    case url: URLClassLoader =>
      url.getURLs.iterator
        .map(url => Paths.get(url.toURI))
        .map(_.toAbsolutePath.toString)
        .toList
    case els => sys.error(s"Expected URLClassloader, obtained $els")
  }
}

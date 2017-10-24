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
    in: String = Paths.get(".").toString,
    @HelpMessage("The output directory to generate the fox site.")
    @ExtraName("o")
    out: String = Paths.get("target").resolve("fox").toString,
    cwd: String = Paths.get(".").toAbsolutePath.toString,
    repoName: String = "olafurpg/fox",
    repoUrl: String = "https://github.com/olafurpg/fox",
    title: String = "Fox",
    stars: Int = 0,
    forks: Int = 0,
    description: String = "My Description",
    classpath: List[String] = Options.defaultClasspath,
    cleanTarget: Boolean = false,
    baseUrl: String = "",
    encoding: String = "UTF-8"
) {

  private val indexMd = Paths.get("index.md")
  private val indexHtml = Paths.get("index.html")

  def asset(path: String) = s"$baseUrl/assets/$path"
  def lib(path: String) = s"$baseUrl/lib/$path"
  def href(doc: Doc): String = {
    if (doc.path == indexMd) s"$baseUrl/"
    else if (doc.path.endsWith(indexMd)) {
      s"$baseUrl/${doc.path.getParent.toString}"
    } else {
      s"$baseUrl/${doc.path.toString}".stripSuffix(".md")
    }
  }
  def charset: Charset = Charset.forName(encoding)
  def resolveIn(relpath: Path): Path = {
    require(!relpath.isAbsolute)
    inPath.resolve(relpath)
  }
  def resolveOut(relpath: Path): Path = {
    require(!relpath.isAbsolute)
    val base = outPath.resolve(relpath)
    if (relpath.endsWith(indexMd)) {
      // foo/specimen.md => foo/index.html
      base.resolveSibling(indexHtml)
    } else {
      // foo/bar.md => foo/bar/index.html
      outPath.resolve(relpath).stripSuffix(".md").resolve(indexHtml)
    }
  }
  def pretty(path: Path): String = cwdPath.relativize(path).toString
  lazy val cwdPath: Path = Paths.get(cwd).toAbsolutePath.normalize()
  lazy val inPath: Path = Paths.get(in).toAbsolutePath.normalize()
  lazy val outPath: Path = Paths.get(out).toAbsolutePath.normalize()
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

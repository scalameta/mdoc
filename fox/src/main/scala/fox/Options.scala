package fox

import java.io.File
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import fox.Enrichments._
import fox.Markdown.Doc
import metaconfig.ConfDecoder
import metaconfig.annotation._
import metaconfig.generic
import metaconfig.generic.Surface

object Enrichments {
  implicit class XtensionPathUrl(val path: Path) extends AnyVal {
    def stripSuffix(suffix: String): Path =
      path.resolveSibling(path.getFileName.toString.stripSuffix(suffix))
  }
}

case class Options(
    @Description("The input directory to generate the fox site.")
    @ExtraName("i")
    in: String = Paths.get("docs").toString,
    @Description("The output directory to generate the fox site.")
    @ExtraName("o")
    out: String = Paths.get("target").resolve("fox").toString,
    cleanTarget: Boolean = false,
    encoding: String = "UTF-8",
    configPath: String = Paths.get("fox.conf").toString
) {

  private final val indexMd = Paths.get("index.md")
  lazy val configFile = Paths.get(configPath)
  lazy val config: Config =
    if (Files.exists(configFile)) Config.fromPath(configFile)
    else Config()

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

  lazy val inPath: Path = Paths.get(in).toAbsolutePath.normalize()
  lazy val outPath: Path = Paths.get(out).toAbsolutePath.normalize()
}

object Options {
  implicit val surface: Surface[Options] =
    generic.deriveSurface[Options]
  implicit val decoder: ConfDecoder[Options] =
    generic.deriveDecoder[Options](Options())
  def defaultClasspath: List[String] = this.getClass.getClassLoader match {
    case url: URLClassLoader =>
      url.getURLs.iterator
        .map(url => Paths.get(url.toURI))
        .map(_.toAbsolutePath.toString)
        .toList
    case els => sys.error(s"Expected URLClassloader, obtained $els")
  }
}

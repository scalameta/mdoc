package scalamd

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

@AppName("scalamd")
@AppVersion("0.1.0-SNAPSHOT")
@ProgName("scalamd")
case class Options(
    @HelpMessage("The input directory to generate the scalamd site.")
    @ExtraName("i")
    in: String = Paths.get(".").toString,
    @HelpMessage("The output directory to generate the scalamd site.")
    @ExtraName("o")
    out: String = Paths.get("target").resolve("scalamd").toString,
    cwd: String = Paths.get(".").toAbsolutePath.toString,
    classpath: List[String] = Options.defaultClasspath,
    encoding: String = "UTF-8"
) {
  private val indexMd = Paths.get("index.md")
  private val indexHtml = Paths.get("index.html")
  def charset: Charset = Charset.forName(encoding)
  def resolveIn(relpath: Path): Path = {
    require(!relpath.isAbsolute)
    inPath.resolve(relpath)
  }
  def resolveOut(relpath: Path): Path = {
    require(!relpath.isAbsolute)
    val base = outPath.resolve(relpath)
    if (relpath.endsWith(indexMd)) {
      // foo/index.md => foo/index.html
      base.resolveSibling(indexHtml)
    } else {
      // foo/bar.md => foo/bar/index.html
      outPath
        .resolve(relpath)
        .resolveSibling(relpath.getFileName.toString.stripSuffix(".md"))
        .resolve(indexHtml)
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

package fox

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.GenSeq
import scala.util.control.NonFatal
import fox.Markdown._
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet

class Runner(
    options: Options,
    mdSettings: MutableDataSet,
    logger: Logger
) {
  private val parser = Parser.builder(mdSettings).build
  private val renderer = HtmlRenderer.builder(mdSettings).build
  def collectInputPaths: GenSeq[Path] = {
    val paths = List.newBuilder[Path]
    Files.walkFileTree(
      options.inPath,
      new SimpleFileVisitor[Path] {
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          if (Files.isRegularFile(file)
            && file.getFileName.toString.endsWith(".md")) {
            paths += options.inPath.relativize(file)
          }
          FileVisitResult.CONTINUE
        }
      }
    )
    paths.result()
  }

  def cleanTarget(): Unit = {
    if (!options.cleanTarget || !Files.exists(options.outPath)) return
    Files.walkFileTree(
      options.outPath,
      new SimpleFileVisitor[Path] {
        override def visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult = {
          Files.delete(file)
          FileVisitResult.CONTINUE
        }
        override def postVisitDirectory(
            dir: Path,
            exc: IOException
        ): FileVisitResult = {
          Files.delete(dir)
          FileVisitResult.CONTINUE
        }
      }
    )
  }

  def handlePath(path: Path): Doc = {
    val source = options.resolveIn(path)
    val compiled = TutCompiler.compile(source, options)
    val md = parser.parse(compiled)
    val html = renderer.render(md)
    val headers = collect[Heading, Header](md) { case h => Header(h) }
    val title = headers
      .find(_.level == 1)
      .getOrElse(sys.error(s"Missing h1 for page $path"))
      .title
    Doc(path, title, headers, html)
  }

  class FileError(path: Path, cause: Throwable)
      extends Exception(path.toString) {
    override def getStackTrace: Array[StackTraceElement] = Array.empty
    override def getCause: Throwable = cause
  }

  def handleSite(site: Site): Unit = {
    val template = new Template(options, logger)
    site.docs.foreach { doc =>
      val html = template.render(doc, site).toString()
//      pprint.log(html)
      val source = options.resolveIn(doc.path)
      val target = options.resolveOut(doc.path)
      Files.createDirectories(target.getParent)
      Files.write(target, html.getBytes(options.charset))
      logger.info(
        s"Compiled ${options.pretty(source)} => ${options.pretty(target)}"
      )
    }
  }

  def copyAssets(): Unit = {
    import ammonite.ops._
    import ammonite.ops.Path
    val paths = List[RelPath](
      "assets" / "images" / "favicon.png",
      "assets" / "javascripts" / "application-268d62d82d.js",
//      "assets" / "javascripts" / "lunr" / "lunr.da.js",
//      "assets" / "javascripts" / "lunr" / "lunr.de.js",
//      "assets" / "javascripts" / "lunr" / "lunr.du.js",
//      "assets" / "javascripts" / "lunr" / "lunr.es.js",
//      "assets" / "javascripts" / "lunr" / "lunr.fi.js",
//      "assets" / "javascripts" / "lunr" / "lunr.fr.js",
//      "assets" / "javascripts" / "lunr" / "lunr.hu.js",
//      "assets" / "javascripts" / "lunr" / "lunr.it.js",
//      "assets" / "javascripts" / "lunr" / "lunr.jp.js",
//      "assets" / "javascripts" / "lunr" / "lunr.multi.js",
//      "assets" / "javascripts" / "lunr" / "lunr.stemmer.support.js",
//      "assets" / "javascripts" / "lunr" / "tinyseg.js",
//      "assets" / "javascripts" / "paradox-material-theme.js",
      "assets" / "stylesheets" / "application-04ea671600.css",
      "assets" / "stylesheets" / "application-23f75ab9c7.palette.css",
      "assets" / "stylesheets" / "paradox-material-theme.css",
      "lib" / "material__tabs" / "dist" / "mdc.tabs.min.css",
      "lib" / "modernizr" / "modernizr.min.js",
      "lib" / "prettify" / "lang-scala.js",
      "lib" / "prettify" / "prettify.css",
      "lib" / "prettify" / "prettify.js"
    )
    paths.foreach { path =>
      val appjs = resource / path
      val x = read.bytes(appjs)
      val out = Path(options.outPath) / path
//      pprint.log(out)
      write.over(out, x)
    }
  }

  def run(): Unit = {
    cleanTarget()
    val paths = collectInputPaths
    if (paths.isEmpty) {
      logger.error(s"${options.inPath} contains no .md files!")
    } else {
      val docs = paths.flatMap { path =>
        try {
          handlePath(path) :: Nil
        } catch {
          case NonFatal(e) =>
            new FileError(path, e).printStackTrace()
            Nil
        }
      }
      handleSite(Site(docs.toList))
      copyAssets()
    }
  }
}

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
    if (!options.cleanTarget) return
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
    val headers = collect[Heading, Header](md) { case h => Header(h) }
    val title = headers
      .find(_.level == 1)
      .getOrElse(sys.error(s"Missing h1 for page $path"))
      .title
    Doc(path, title, headers, md)
  }

  class FileError(path: Path, cause: Throwable)
      extends Exception(path.toString) {
    override def getStackTrace: Array[StackTraceElement] = Array.empty
    override def getCause: Throwable = cause
  }

  def handleDoc(doc: Doc): Unit = {
    val html = renderer.render(doc.body)
    val target = options.resolveOut(doc.path)
    Files.createDirectories(target.getParent)
    Files.write(target, html.getBytes(options.charset))
    logger.info(
      s"Compiled ${options.pretty(target)} => ${options.pretty(target)}"
    )
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
      docs.foreach(handleDoc)
    }
  }
}

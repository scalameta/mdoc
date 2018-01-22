package fox

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

import fox.Markdown._
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet

final class Runner(
    options: Options,
    mdSettings: MutableDataSet,
    logger: Logger
) {
  private final val parser = Parser.builder(mdSettings).build
  private final val formatter = Formatter.builder(mdSettings).build
  def collectInputPaths: List[Path] = {
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

  val siteVariables = Map(
    "site.version" -> "1.0.0"
  )

  import scala.util.Try
  def handlePath(path: Path): Try[Doc] = Try {
    val source = options.resolveIn(path)
    val compiled = TutCompiler.compile(source, options)
    val ast = parser.parse(compiled)
    val md = formatter.render(ast)
    val headers = collect[Heading, Header](ast) { case h => Header(h) }
    val title = headers
      .find(_.level == 1)
      .getOrElse(sys.error(s"Missing h1 for page $path"))
      .title
    Doc(path, title, headers, md)
  }

  def writePath(path: Path, string: String): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, string.getBytes(options.charset))
  }

  private class FileError(path: Path, cause: Throwable)
    extends Exception(path.toString) {
    override def getStackTrace: Array[StackTraceElement] = Array.empty
    override def getCause: Throwable = cause
  }

  def run(): Unit = {
    cleanTarget()
    val paths = collectInputPaths
    if (paths.isEmpty) {
      logger.error(s"${options.inPath} contains no .md files!")
    } else {
      import scala.util.{Success, Failure}
      val docs = paths.flatMap { path =>
        handlePath(path) match {
          case Success(doc) => doc :: Nil
          case Failure(t) => new FileError(path, t).printStackTrace(); Nil
        }
      }

      docs.foreach { doc =>
        val source = options.resolveIn(doc.path)
        val target = options.resolveOut(doc.path)
        writePath(target, doc.contents)
        logger.info(s"Markdown file ${target} has been generated.")
      }
    }
  }
}

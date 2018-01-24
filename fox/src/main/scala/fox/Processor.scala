package fox

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import fox.Markdown._
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.MutableDataSet

final class Processor(
    options: Options,
    mdSettings: MutableDataSet,
    logger: Logger
) {
  private final val parser = Parser.builder(mdSettings).build
  private final val formatter = Formatter.builder(mdSettings).build

  import scala.util.Try
  def handlePath(path: Path): Try[Doc] = Try {
    val sourcePath = options.resolveIn(path)
    val source = new String(java.nio.file.Files.readAllBytes(sourcePath), StandardCharsets.UTF_8)
    val ast = parser.parse(source)
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

  private class FileError(path: Path, cause: Throwable) extends Exception(path.toString) {
    override def getStackTrace: Array[StackTraceElement] = Array.empty
    override def getCause: Throwable = cause
  }

  import fox.utils.IO
  def run(): Unit = {
    IO.cleanTarget(options)
    val paths = IO.collectInputPaths(options)
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

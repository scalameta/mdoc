package vork

import java.nio.file.Files
import java.nio.file.Path

import scala.util.Try
import scala.util.control.NoStackTrace
import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.{DataKey, MutableDataSet}
import vork.utils.SourceWatcher
import io.methvin.watcher.DirectoryChangeEvent
import vork.Markdown.Doc

final class Processor(
    options: Options,
    mdSettings: MutableDataSet,
    logger: Logger
) {
  require(
    options.isAbsolute,
    s"Options contains relative paths. " +
      s"Use Options.fromDefault($options) to absolutize paths."
  )

  def handlePath(path: Path): Doc = {
    val sourcePath = options.resolveIn(path)
    val source = new String(java.nio.file.Files.readAllBytes(sourcePath), options.encoding)
    mdSettings.set(Processor.PathKey, Some(path))
    val parser = Parser.builder(mdSettings).build
    val formatter = Formatter.builder(mdSettings).build
    val ast = parser.parse(source)
    val md = formatter.render(ast)
    Doc(path, md)
  }

  def writePath(path: Path, string: String): Unit = {
    Files.createDirectories(path.getParent)
    Files.write(path, string.getBytes(options.encoding))
  }

  private class FileError(path: Path, cause: Throwable)
      extends Exception(path.toString)
      with NoStackTrace {
    override def getCause: Throwable = cause
  }

  import vork.utils.IO

  def triggerGeneration(): Unit = {
    val paths = IO.collectInputPaths(options)
    if (paths.isEmpty) {
      logger.error(s"${options.in} contains no .md files!")
    } else {
      import scala.util.{Success, Failure}
      val docs = paths.flatMap { path =>
        Try(handlePath(path)) match {
          case Success(doc) => doc :: Nil
          case Failure(t) => new FileError(path, t).printStackTrace(); Nil
        }
      }

      docs.foreach { doc =>
        val target = options.resolveOut(doc.path)
        writePath(target, doc.contents)
        logger.info(s"Markdown file $target has been generated.")
      }
    }
  }

  def run(): Unit = {
    IO.cleanTarget(options)
    if (!options.watch) triggerGeneration()
    else {
      triggerGeneration()
      SourceWatcher.watch(
        List(options.in),
        (event: DirectoryChangeEvent) => {
          // Let's make this info for now, we can turn it into debug afterwards
          logger.info(
            s"Event ${event.eventType()} at ${event.path()} triggered markdown generation."
          )
          triggerGeneration()
        }
      )
    }
  }

}

object Processor {
  val PathKey = new DataKey[Option[Path]]("originPath", None)
}

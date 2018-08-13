package vork

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.{DataKey, MutableDataSet}
import vork.utils.SourceWatcher
import io.methvin.watcher.DirectoryChangeEvent
import org.langmeta.internal.io.PathIO
import vork.utils.IO
import scala.meta.Input

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

  def handleMarkdown(path: Path): Unit = {
    logger.reset()
    val sourcePath = options.resolveIn(path)
    val source = new String(java.nio.file.Files.readAllBytes(sourcePath), options.encoding)
    mdSettings.set(Processor.PathKey, Some(path))
    val parser = Parser.builder(mdSettings).build
    val formatter = Formatter.builder(mdSettings).build
    val ast = parser.parse(source)
    val md = formatter.render(ast)
    val target = options.resolveOut(path)
    if (logger.hasErrors) {
      logger.error(s"Failed to generate $target")
    } else {
      writePath(target, md)
      logger.info(s"Generated $target")
    }
  }

  def handleRegularFile(path: Path): Unit = {
    val source = options.resolveIn(path)
    val target = options.resolveOut(path)
    Files.createDirectories(target.getParent)
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
    logger.info(s"Copied    $target")
  }

  def handlePath(path: Path): Unit = {
    def run(path: Path): Unit = {
      if (!options.matcher.matches(path)) ()
      else {
        PathIO.extension(path) match {
          case "md" => handleMarkdown(path)
          case _ => handleRegularFile(path)
        }
      }
    }
    try {
      run(options.in.relativize(path))
    } catch {
      case NonFatal(e) =>
        new FileError(path, e).printStackTrace()
    }
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

  def generateCompleteSite(): Unit = {
    val paths = IO.collectInputPaths(options)
    if (paths.isEmpty) {
      logger.error(s"${options.in} contains no files!")
    } else {
      paths.foreach(handlePath)
    }
  }

  def run(): Unit = {
    IO.cleanTarget(options)
    generateCompleteSite()
    if (options.watch) {
      SourceWatcher.watch(
        List(options.in),
        (event: DirectoryChangeEvent) => {
          handlePath(event.path())
        }
      )
    }
  }

}

object Processor {
  val PathKey = new DataKey[Option[Path]]("originPath", None)
  val InputKey = new DataKey[Option[Input.VirtualFile]]("originPath", None)
}

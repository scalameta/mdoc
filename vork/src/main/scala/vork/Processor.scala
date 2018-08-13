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
import org.langmeta.internal.io.FileIO
import org.langmeta.internal.io.PathIO
import org.langmeta.io.AbsolutePath
import vork.utils.IO
import scala.meta.Input

final class Processor(
    options: Args,
    mdSettings: MutableDataSet,
    logger: Logger
) {

  def handleMarkdown(file: InputFile): Unit = {
    logger.reset()
    val source = FileIO.slurp(file.in, options.encoding)
    mdSettings.set(Processor.PathKey, Some(file.in.toNIO))
    val parser = Parser.builder(mdSettings).build
    val formatter = Formatter.builder(mdSettings).build
    val ast = parser.parse(source)
    val md = formatter.render(ast)
    if (logger.hasErrors) {
      logger.error(s"Failed to generate ${file.out}")
    } else {
      writePath(file, md)
      logger.info(s"Generated ${file.out}")
    }
  }

  def handleRegularFile(file: InputFile): Unit = {
    Files.createDirectories(file.out.toNIO.getParent)
    Files.copy(file.in.toNIO, file.out.toNIO, StandardCopyOption.REPLACE_EXISTING)
    logger.info(s"Copied    ${file.out.toNIO}")
  }

  def handleFile(file: InputFile): Unit = {
    try {
      if (!options.matches(file.relpath)) ()
      else {
        PathIO.extension(file.in.toNIO) match {
          case "md" => handleMarkdown(file)
          case _ => handleRegularFile(file)
        }
      }
    } catch {
      case NonFatal(e) =>
        new FileError(file.in, e).printStackTrace()
    }
  }

  def writePath(file: InputFile, string: String): Unit = {
    Files.createDirectories(file.out.toNIO.getParent)
    Files.write(file.out.toNIO, string.getBytes(options.encoding))
  }

  private class FileError(path: AbsolutePath, cause: Throwable)
      extends Exception(path.toString)
      with NoStackTrace {
    override def getCause: Throwable = cause
  }

  def generateCompleteSite(): Unit = {
    var isEmpty = true
    IO.foreachFile(options) { file =>
      isEmpty = false
      handleFile(file)
    }
    if (isEmpty) {
      logger.error(s"no input files: ${options.in}")
    }
  }

  def run(): Unit = {
    IO.cleanTarget(options)
    generateCompleteSite()
    if (options.watch) {
      SourceWatcher.watch(
        List(options.in.toNIO),
        (event: DirectoryChangeEvent) => {
          options.toInputFile(AbsolutePath(event.path())) match {
            case Some(inputFile) => handleFile(inputFile)
            case None => ()
          }
        }
      )
    }
  }

}

object Processor {
  val PathKey = new DataKey[Option[Path]]("originPath", None)
  val InputKey = new DataKey[Option[Input.VirtualFile]]("originPath", None)
}

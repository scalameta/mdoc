package vork.internal.cli

import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.DataKey
import com.vladsch.flexmark.util.options.MutableDataSet
import io.methvin.watcher.DirectoryChangeEvent
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import scala.meta.Input
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import vork.internal.io.IO
import vork.internal.io.Logger
import vork.internal.io.FileWatcher

final class MainOps(
                     options: Settings,
                     mdSettings: MutableDataSet,
                     logger: Logger
) {

  def handleMarkdown(file: InputFile): Unit = {
    logger.reset()
    val source = FileIO.slurp(file.in, options.encoding)
    val input = Input.VirtualFile(file.in.toString(), source)
    mdSettings.set(MainOps.InputKey, Some(input))
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
      FileWatcher.watch(
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

object MainOps {
  val InputKey = new DataKey[Option[Input.VirtualFile]]("scalametaInput", None)
}

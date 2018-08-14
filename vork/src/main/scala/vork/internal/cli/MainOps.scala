package vork.internal.cli

import com.vladsch.flexmark.formatter.internal.Formatter
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.options.DataKey
import com.vladsch.flexmark.util.options.MutableDataSet
import io.methvin.watcher.DirectoryChangeEvent
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import scala.meta.Input
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.util.control.NoStackTrace
import scala.util.control.NonFatal
import vork.Reporter
import vork.internal.io.IO
import vork.internal.io.FileWatcher
import vork.internal.markdown.Markdown

final class MainOps(
    settings: Settings,
    markdown: MutableDataSet,
    reporter: Reporter
) {

  def handleMarkdown(file: InputFile): Unit = {
    reporter.reset()
    reporter.info(s"Compiling ${file.in}")
    val start = System.nanoTime()
    val source = FileIO.slurp(file.in, settings.encoding)
    val input = Input.VirtualFile(file.in.toString(), source)
    markdown.set(MainOps.InputKey, Some(input))
    val md = Markdown.toMarkdown(input, markdown, reporter)
    if (reporter.hasErrors) {
      reporter.error(s"Failed to generate ${file.out}")
    } else {
      writePath(file, md)
      val end = System.nanoTime()
      val elapsed = TimeUnit.NANOSECONDS.toMillis(end - start)
      reporter.info(f"  done => ${file.out} ($elapsed%,d ms)")
    }
  }

  def handleRegularFile(file: InputFile): Unit = {
    Files.createDirectories(file.out.toNIO.getParent)
    Files.copy(file.in.toNIO, file.out.toNIO, StandardCopyOption.REPLACE_EXISTING)
    reporter.info(s"Copied    ${file.out.toNIO}")
  }

  def handleFile(file: InputFile): Unit = {
    try {
      if (!settings.matches(file.relpath)) ()
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
    Files.write(file.out.toNIO, string.getBytes(settings.encoding))
  }

  private class FileError(path: AbsolutePath, cause: Throwable)
      extends Exception(path.toString)
      with NoStackTrace {
    override def getCause: Throwable = cause
  }

  def generateCompleteSite(): Unit = {
    var isEmpty = true
    IO.foreachFile(settings) { file =>
      isEmpty = false
      handleFile(file)
    }
    if (isEmpty) {
      reporter.error(s"no input files: ${settings.in}")
    }
  }

  def run(): Unit = {
    IO.cleanTarget(settings)
    generateCompleteSite()
    if (settings.watch) {
      FileWatcher.watch(
        List(settings.in.toNIO),
        (event: DirectoryChangeEvent) => {
          settings.toInputFile(AbsolutePath(event.path())) match {
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
  val VariablesKey = new DataKey[Option[Map[String, String]]]("siteVariables", None)
}

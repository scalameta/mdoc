package vork.internal.cli

import com.vladsch.flexmark.util.options.MutableDataSet
import io.methvin.watcher.DirectoryChangeEvent
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import metaconfig.Configured
import scala.meta.Input
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.util.control.NonFatal
import vork.Reporter
import vork.internal.io.FileWatcher
import vork.internal.io.IO
import vork.internal.markdown.Markdown

final class MainOps(
    settings: Settings,
    markdown: MutableDataSet,
    reporter: Reporter
) {

  def handleMarkdown(file: InputFile): Unit = synchronized {
    reporter.reset()
    reporter.info(s"Compiling ${file.in}")
    val start = System.nanoTime()
    val source = FileIO.slurp(file.in, settings.charset)
    val input = Input.VirtualFile(file.in.toString(), source)
    markdown.set(Markdown.InputKey, Some(input))
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

  def handleWatchEvent(event: DirectoryChangeEvent): Unit = {
    val path = AbsolutePath(event.path())
    settings.toInputFile(path) match {
      case Some(inputFile) => handleFile(inputFile)
      case None => ()
    }
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
        new FileException(file.in, e).printStackTrace()
    }
  }

  def writePath(file: InputFile, string: String): Unit = {
    Files.createDirectories(file.out.toNIO.getParent)
    Files.write(file.out.toNIO, string.getBytes(settings.charset))
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
    if (settings.cleanTarget && Files.exists(settings.out.toNIO)) {
      IO.cleanTarget(settings.out)
    }
    generateCompleteSite()
    if (settings.watch) {
      FileWatcher.watch(settings.in, handleWatchEvent)
    }
  }

}

object MainOps {
  def process(context: Configured[Context], reporter: Reporter): Int = {
    context match {
      case Configured.NotOk(error) =>
        error.all.foreach(message => reporter.error(message))
        1
      case Configured.Ok(ctx) =>
        val markdown = Markdown.default(ctx)
        val runner = new MainOps(ctx.settings, markdown, ctx.reporter)
        runner.run()
        if (ctx.reporter.hasErrors) {
          1 // error
        } else {
          0
        }
    }
  }
}

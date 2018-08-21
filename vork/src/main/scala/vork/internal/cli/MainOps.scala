package vork.internal.cli

import com.vladsch.flexmark.util.options.MutableDataSet
import io.methvin.watcher.DirectoryChangeEvent
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import metaconfig.Configured
import scala.meta.Input
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.util.control.NonFatal
import scalafix.internal.diff.DiffUtils
import vork.Reporter
import vork.internal.io.IO
import vork.internal.io.VorkFileListener
import vork.internal.markdown.Markdown
import vork.internal.markdown.MarkdownLinks
import vork.internal.markdown.MarkdownLinter

final class MainOps(
    settings: Settings,
    markdown: MutableDataSet,
    reporter: Reporter
) {

  def lint(): Unit = {
    val links = MarkdownLinks.fromGeneratedSite(settings, reporter)
    MarkdownLinter.lint(links, reporter)
  }

  def handleMarkdown(file: InputFile): Unit = synchronized {
    clearScreen()
    reporter.reset()
    reporter.info(s"Compiling ${file.in}")
    val start = System.nanoTime()
    val source = FileIO.slurp(file.in, settings.charset)
    val input = Input.VirtualFile(file.in.toString(), source)
    markdown.set(Markdown.InputKey, Some(input))
    val md = Markdown.toMarkdown(input, markdown, reporter, settings)
    if (reporter.hasErrors) {
      reporter.error(s"Failed to generate ${file.out}")
    } else {
      writePath(file, md)
      val end = System.nanoTime()
      val elapsed = TimeUnit.NANOSECONDS.toMillis(end - start)
      reporter.info(f"  done => ${file.out} ($elapsed%,d ms)")
      waitingForFileChanges()
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
        new FileException(file.in, e).printStackTrace()
    }
  }

  def writePath(file: InputFile, string: String): Unit = {
    if (settings.test) {
      if (!file.out.isFile) return
      val expected = FileIO.slurp(file.out, settings.charset)
      if (expected != string) {
        val filename = file.out.toString()
        val diff = DiffUtils.unifiedDiff(
          s"$filename (on disk)",
          s"$filename (expected output)",
          expected.lines.toList,
          string.lines.toList,
          3
        )
        reporter.error(s"--test failed! To fix this problem, re-generate the documentation\n$diff")
      }
    } else {
      Files.createDirectories(file.out.toNIO.getParent)
      Files.write(file.out.toNIO, string.getBytes(settings.charset))
    }
  }

  def generateCompleteSite(): Unit = {
    var isEmpty = true
    IO.foreachInputFile(settings) { file =>
      isEmpty = false
      handleFile(file)
    }
    if (isEmpty) {
      reporter.error(s"no input files: ${settings.in}")
    }
    lint()
  }

  def run(): Unit = {
    if (settings.cleanTarget && Files.exists(settings.out.toNIO)) {
      IO.cleanTarget(settings.out)
    }
    generateCompleteSite()
    runFileWatcher()
  }

  def handleWatchEvent(event: DirectoryChangeEvent): Unit = {
    val path = AbsolutePath(event.path())
    settings.toInputFile(path) match {
      case Some(inputFile) => handleFile(inputFile)
      case None => ()
    }
    lint()
  }

  def runFileWatcher(): Unit = {
    if (settings.isFileWatching) {
      val executor = Executors.newFixedThreadPool(1)
      val watcher = VorkFileListener.create(settings.in, executor, System.in)(handleWatchEvent)
      watcher.watchUntilInterrupted()
    }
  }

  def clearScreen(): Unit = {
    if (settings.isFileWatching) {
      print("\033[H\033[2J")
    }
  }

  def waitingForFileChanges(): Unit = {
    if (settings.isFileWatching) {
      reporter.println(s"Waiting for file changes (press enter to interrupt)")
    }
  }

}

object MainOps {
  def process(settings: Configured[Settings], reporter: Reporter): Int = {
    settings match {
      case Configured.Ok(setting) if setting.help =>
        reporter.println(Settings.help.helpMessage(80))
        0
      case Configured.Ok(setting) if setting.usage =>
        reporter.println(Settings.usage)
        0
      case Configured.Ok(setting) if setting.version =>
        reporter.println(Settings.version)
        0
      case els =>
        els.andThen(_.validate(reporter)) match {
          case Configured.NotOk(error) =>
            error.all.foreach(message => reporter.error(message))
            1
          case Configured.Ok(ctx) =>
            val markdown = Markdown.vorkSettings(ctx)
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
}

package mdoc.internal.cli

import com.vladsch.flexmark.util.options.MutableDataSet
import io.methvin.watcher.DirectoryChangeEvent
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import mdoc.Reporter
import mdoc.internal.BuildInfo
import mdoc.internal.io.IO
import mdoc.internal.io.MdocFileListener
import mdoc.internal.livereload.LiveReload
import mdoc.internal.livereload.UndertowLiveReload
import mdoc.internal.markdown.DocumentLinks
import mdoc.internal.markdown.LinkHygiene
import mdoc.internal.markdown.Markdown
import mdoc.internal.pos.DiffUtils
import metaconfig.Configured
import scala.meta.Input
import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.util.control.NonFatal

final class MainOps(
    settings: Settings,
    markdown: MutableDataSet,
    reporter: Reporter
) {

  private var livereload: Option[LiveReload] = None
  private def startLivereload(): Unit = {
    if (settings.noLivereload) ()
    else {
      val livereload = UndertowLiveReload(
        settings.out.toNIO,
        host = settings.host,
        preferredPort = settings.port,
        reporter = reporter
      )
      livereload.start()
      this.livereload = Some(livereload)
    }
  }

  def lint(): Unit = {
    if (settings.out.isDirectory && !settings.noLinkHygiene) {
      val docs = DocumentLinks.fromGeneratedSite(settings, reporter)
      LinkHygiene.lint(docs, reporter, settings.verbose)
    }
  }

  def handleMarkdown(file: InputFile): Exit = synchronized {
    val originalErrors = reporter.errorCount
    if (settings.verbose) {
      reporter.info(s"Compiling ${file.in}")
    }
    val timer = new Timer
    val source = FileIO.slurp(file.in, settings.charset)
    val input = Input.VirtualFile(file.in.toString(), source)
    markdown.set(Markdown.InputKey, Some(input))
    markdown.set(Markdown.RelativePathKey, Some(file.relpath))
    val md = Markdown.toMarkdown(input, markdown, reporter, settings)
    val fileHasErrors = reporter.errorCount > originalErrors
    if (!fileHasErrors) {
      writePath(file, md)
      if (settings.verbose) {
        reporter.info(f"  done => ${file.out} ($timer)")
      }
      livereload.foreach(_.reload(file.out.toNIO))
    }
    if (reporter.hasErrors) Exit.error
    else Exit.success
  }

  def handleRegularFile(file: InputFile): Exit = {
    Files.createDirectories(file.out.toNIO.getParent)
    Files.copy(file.in.toNIO, file.out.toNIO, StandardCopyOption.REPLACE_EXISTING)
    if (settings.verbose) {
      reporter.info(s"Copied    ${file.out.toNIO}")
    }
    Exit.success
  }

  def handleFile(file: InputFile): Exit = {
    try {
      if (!settings.isIncluded(file.relpath)) Exit.success
      else {
        PathIO.extension(file.in.toNIO) match {
          case "md" => handleMarkdown(file)
          case _ => handleRegularFile(file)
        }
      }
    } catch {
      case NonFatal(e) =>
        new FileException(file.in, e).printStackTrace()
        Exit.error
    }
  }

  def writePath(file: InputFile, string: String): Unit = {
    if (settings.check) {
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

  def generateCompleteSite(): Exit = {
    val files = IO.inputFiles(settings)
    val timer = new Timer()
    val n = files.length
    compilingFiles(n)
    val exit = files.foldLeft(Exit.success) {
      case (accum, file) =>
        val fileExit = handleFile(file)
        accum.merge(fileExit)
    }
    lint()
    if (files.isEmpty) {
      reporter.error(s"no input files: ${settings.in}")
    } else {
      compiledFiles(n, timer)
    }
    exit
  }

  def run(): Exit = {
    if (settings.cleanTarget && Files.exists(settings.out.toNIO)) {
      IO.cleanTarget(settings.out)
    }
    if (settings.watch) {
      startLivereload()
    }
    val isOk = generateCompleteSite()
    if (settings.isFileWatching) {
      waitingForFileChanges()
      runFileWatcher()
      // exit code doesn't matter when file watching
      Exit.success
    } else {
      isOk
    }
  }

  def handleWatchEvent(event: DirectoryChangeEvent): Unit = {
    if (PathIO.extension(event.path()) == "md") {
      clearScreen()
    }
    val path = AbsolutePath(event.path())
    settings.toInputFile(path) match {
      case Some(inputFile) =>
        reporter.reset()
        val timer = new Timer()
        compilingFiles(1)
        handleFile(inputFile)
        lint()
        compiledFiles(1, timer)
        waitingForFileChanges()
      case None => ()
    }
  }

  def runFileWatcher(): Unit = {
    val executor = Executors.newFixedThreadPool(1)
    val watcher = MdocFileListener.create(settings.in, executor, System.in)(handleWatchEvent)
    watcher.watchUntilInterrupted()
    this.livereload.foreach(_.stop())
  }

  def clearScreen(): Unit = {
    print("\033[H\033[2J")
  }

  def waitingForFileChanges(): Unit = {
    reporter.println(s"Waiting for file changes (press enter to interrupt)")
  }

  def compiledFiles(n: Int, timer: Timer): Unit = {
    val errors = Messages.count("error", reporter.errorCount)
    val warnings =
      if (reporter.hasWarnings) {
        s", " + Messages.count("warning", reporter.warningCount)
      } else {
        ""
      }
    reporter.info(s"Compiled in $timer ($errors$warnings)")
  }

  def compilingFiles(n: Int): Unit = {
    val files = Messages.count("file", n)
    reporter.info(s"Compiling $files to ${settings.out}")
  }

}

object MainOps {
  def process(settings: Configured[Settings], reporter: Reporter): Int = {
    settings match {
      case Configured.Ok(setting) if setting.help =>
        reporter.println(Settings.help(BuildInfo.version, 80))
        0
      case Configured.Ok(setting) if setting.usage =>
        reporter.println(Settings.usage)
        0
      case Configured.Ok(setting) if setting.version =>
        reporter.println(Settings.version(BuildInfo.version))
        0
      case els =>
        els.andThen(_.validate(reporter)) match {
          case Configured.NotOk(error) =>
            error.all.foreach(message => reporter.error(message))
            1
          case Configured.Ok(ctx) =>
            val markdown = Markdown.mdocSettings(ctx)
            val runner = new MainOps(ctx.settings, markdown, ctx.reporter)
            val exit = runner.run()
            if (exit.isSuccess) {
              0
            } else {
              1 // error
            }
        }
    }
  }
}

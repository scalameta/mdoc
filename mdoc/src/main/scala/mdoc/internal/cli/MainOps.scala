package mdoc.internal.cli

import com.vladsch.flexmark.parser.Parser
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
import io.methvin.watcher.hashing.FileHasher
import scala.collection.concurrent.TrieMap
import java.nio.file.Path
import io.methvin.watcher.hashing.HashCode
import scala.collection.mutable

final class MainOps(
    context: Context
) {
  def settings: Settings = context.settings
  def reporter: Reporter = context.reporter

  private var livereload: Option[LiveReload] = None
  private def startLivereload(): Unit = {
    if (settings.noLivereload) ()
    else {
      settings.out match {
        case Nil =>
          reporter.error(
            "Can't start LiveReload server since --out is empty. To fix this problem, specify an --out argument."
          )
        case out :: tail =>
          if (tail.nonEmpty) {
            reporter.warning(
              s"Starting LiveReload server at directory $out and ignoring --out value(s) ${tail.mkString(", ")}. " +
                "To LiveReload another directory, place that directory as the first --out argument."
            )
          }
          val livereload = UndertowLiveReload(
            out.toNIO,
            host = settings.host,
            preferredPort = settings.port,
            reporter = reporter
          )
          livereload.start()
          this.livereload = Some(livereload)
      }
    }
  }

  def lint(): Unit = {
    settings.out.foreach { out =>
      if (out.isDirectory && !settings.noLinkHygiene) {
        val docs = DocumentLinks.fromGeneratedSite(settings, reporter)
        LinkHygiene.lint(docs, reporter, settings.verbose)
      }
    }
  }

  def handleMarkdown(file: InputFile): Exit =
    synchronized {
      val originalErrors = reporter.errorCount
      if (settings.verbose) {
        reporter.info(s"Compiling ${file.inputFile}")
      }
      val timer = new Timer
      val source = FileIO.slurp(file.inputFile, settings.charset)
      val input = Input.VirtualFile(file.inputFile.toString(), source)
      val md = Markdown.toMarkdown(input, context, file, settings.site, reporter, settings)
      val fileHasErrors = reporter.errorCount > originalErrors
      if (!fileHasErrors) {
        writePath(file, md)
        if (settings.verbose) {
          reporter.info(f"  done => ${file.outputFile} ($timer)")
        }
        livereload.foreach(_.reload(file.outputFile.toNIO))
      }
      if (reporter.hasErrors) Exit.error
      else Exit.success
    }

  def handleRegularFile(file: InputFile): Exit = {
    Files.createDirectories(file.outputFile.toNIO.getParent)
    Files.copy(file.inputFile.toNIO, file.outputFile.toNIO, StandardCopyOption.REPLACE_EXISTING)
    if (settings.verbose) {
      reporter.info(s"Copied    ${file.outputFile.toNIO}")
    }
    Exit.success
  }

  def handleFile(file: InputFile): Exit = {
    try {
      if (!settings.isIncluded(file.relpath)) Exit.success
      else {
        val extension = PathIO.extension(file.inputFile.toNIO)
        if (settings.isMarkdownFileExtension(extension)) {
          handleMarkdown(file)
        } else {
          handleRegularFile(file)
        }
      }
    } catch {
      case NonFatal(e) =>
        new FileException(file.inputFile, e).printStackTrace()
        Exit.error
    }
  }

  def writePath(file: InputFile, string: String): Unit = {
    if (settings.check) {
      if (!file.outputFile.isFile) return
      val expected = FileIO.slurp(file.outputFile, settings.charset)
      if (expected != string) {
        val filename = file.outputFile.toString()
        val diff = DiffUtils.unifiedDiff(
          s"$filename (on disk)",
          s"$filename (expected output)",
          expected.linesIterator.toList,
          string.linesIterator.toList,
          3
        )
        reporter.error(s"--test failed! To fix this problem, re-generate the documentation\n$diff")
      }
    } else if (file.outputFile.isDirectory) {
      reporter.error(
        s"can't write output file '${file.outputFile}' because it's a directory. " +
          "To fix this problem, either remove this directory or point the --out argument to another path."
      )
    } else {
      Files.createDirectories(file.outputFile.toNIO.getParent)
      Files.write(file.outputFile.toNIO, string.getBytes(settings.charset))
    }
  }

  def generateCompleteSite(): Exit = {
    // NOTE(olafur): we sort the input files for reproducible output in cases
    // where multiple input files write to the same output file.
    val files = IO.inputFiles(settings).sorted
    val timer = new Timer()
    val n = files.length
    compilingFiles(n)
    val exit = files.foldLeft(Exit.success) { case (accum, file) =>
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
    settings.out.foreach { out =>
      if (settings.cleanTarget && Files.exists(out.toNIO)) {
        IO.cleanTarget(out)
      }
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

  val hashes = mutable.Map.empty[Path, HashCode]
  def handleWatchEvent(event: DirectoryChangeEvent): Unit = {
    val path = AbsolutePath(event.path())
    settings.toInputFile(path) match {
      case Some(inputFile) =>
        hashes.synchronized {
          val oldHash = hashes.get(event.path())
          val newHash = FileHasher.DEFAULT_FILE_HASHER.hash(event.path())
          if (!oldHash.contains(newHash)) {
            if (PathIO.extension(event.path()) == "md") {
              clearScreen()
            }
            hashes.put(event.path(), newHash)
            reporter.reset()
            val timer = new Timer()
            compilingFiles(1)
            handleFile(inputFile)
            lint()
            compiledFiles(1, timer)
            waitingForFileChanges()
          }
        }
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
    print("\u001b[H\u001b[2J")
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
    reporter.info(s"Compiling $files to ${settings.out.mkString(", ")}")
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
        els.andThen(s => Context.fromSettings(s, reporter)) match {
          case Configured.NotOk(error) =>
            error.all.foreach(message => reporter.error(message))
            1
          case Configured.Ok(ctx) =>
            if (ctx.settings.verbose) {
              ctx.reporter.setDebugEnabled(true)
            }
            val runner = new MainOps(ctx)
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

package mdoc

import java.io.PrintStream
import java.nio.file.Path
import metaconfig.Configured
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import mdoc.internal.cli.MainOps
import mdoc.internal.cli.Settings
import mdoc.internal.io.ConsoleReporter
import mdoc.internal.markdown.Markdown

object Main extends MainProcess {
  def main(args: Array[String]): Unit = {
    val code = process(args, System.out, PathIO.workingDirectory.toNIO)
    if (code != 0) sys.exit(code)
  }
}

object SbtMain extends MainProcess {
  def main(args: Array[String]): Unit = {
    val code = process(args, System.out, PathIO.workingDirectory.toNIO)
    if (code != 0) sys.error("mdoc failed")
  }
}

trait MainProcess {
  def process(args: Array[String], out: PrintStream, cwd: Path): Int = {
    process(args, new ConsoleReporter(out), cwd)
  }
  def process(args: Array[String], reporter: Reporter, cwd: Path): Int = {
    val base = Settings.default(AbsolutePath(cwd))
    val ctx = Settings.fromCliArgs(args.toList, base)
    MainOps.process(ctx, reporter)
  }
  def process(settings: MainSettings): Int = {
    MainOps.process(Configured.ok(settings.settings), settings.reporter)
  }
}

package vork

import java.io.PrintStream
import java.nio.file.Path
import metaconfig.Configured
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import vork.internal.cli.MainOps
import vork.internal.cli.Settings
import vork.internal.io.ConsoleReporter
import vork.internal.markdown.Markdown

object Main {

  def main(args: Array[String]): Unit = {
    val code = process(args, System.out, PathIO.workingDirectory.toNIO)
    sys.exit(code)
  }

  def process(args: Array[String], out: PrintStream, cwd: Path): Int = {
    process(args, new ConsoleReporter(out), cwd)
  }
  def process(args: Array[String], reporter: Reporter, cwd: Path): Int = {
    val base = Settings.default(AbsolutePath(cwd))
    val ctx = Settings.fromCliArgs(args.toList, base).andThen(_.validate(reporter))
    MainOps.process(ctx, reporter)
  }

  def process(settings: MainSettings): Int = {
    MainOps.process(settings.settings.validate(settings.reporter), settings.reporter)
  }

}

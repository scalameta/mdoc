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

/** This main is meant to be called from a running JVM. Rather than exiting with a non-zero code on
  * error, it will throw an exception. This is useful when calling from another JVM and you don't
  * want this JVM to exit and mdoc fails.
  *
  * This is the case for the `sbt` integration. When an exception is raised, `sbt` will make the
  * task as failed but won't exit. This provides a better user experience.
  */
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
    val ctx = Settings.fromCliArgs(args.toList, cwd)
    MainOps.process(ctx, reporter)
  }
  def process(settings: MainSettings): Int = {
    MainOps.process(Configured.ok(settings.settings), settings.reporter)
  }
}

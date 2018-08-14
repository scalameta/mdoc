package vork

import java.io.PrintStream
import java.nio.file.Path
import metaconfig.Configured
import scala.meta.inputs.Position
import scala.meta.internal.io.PathIO
import scala.meta.io.AbsolutePath
import scala.tools.nsc.interpreter.OutputStream
import vork.internal.cli.Settings
import vork.internal.cli.MainOps
import vork.internal.io.ConsoleReporter
import vork.internal.markdown.Markdown

object Main {
  def main(args: Array[String]): Unit = {
    sys.exit(process(args, System.out, PathIO.workingDirectory.toNIO))
  }

  def process(
      args: Array[String],
      stdout: OutputStream,
      cwd: Path
  ): Int = {
    val out = new PrintStream(stdout)
    val reporter = new ConsoleReporter(out)
    val base = Settings.default(AbsolutePath(cwd))
    Settings.fromCliArgs(args.toList, reporter, base) match {
      case Configured.NotOk(error) =>
        pprint.log(error)
        pprint.log(error.all)
        reporter.error("foooo")
        error.all.foreach(message => reporter.error(message))
        1
      case Configured.Ok(context) =>
        val markdown = Markdown.default(context)
        val runner = new MainOps(context.settings, markdown, reporter)
        runner.run()
        if (context.reporter.hasErrors) {
          1 // error
        } else {
          out.println(context.settings.out.toString)
          0
        }
    }
  }
}

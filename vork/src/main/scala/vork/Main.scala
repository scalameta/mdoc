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
import vork.internal.io.ConsoleLogger
import vork.internal.markdown.Markdown

trait Logger {
  def hasErrors: Boolean
  def reset(): Unit
  def error(pos: Position, throwable: Throwable): Unit
  def error(pos: Position, msg: String): Unit
  def error(msg: String): Unit
  def info(pos: Position, msg: String): Unit
  def info(msg: String): Unit
  def warning(pos: Position, msg: String): Unit
  def warning(msg: String): Unit
}

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
    val logger = new ConsoleLogger(out)
    val base = Settings.default(AbsolutePath(cwd))
    Settings.fromCliArgs(args.toList, logger, base) match {
      case Configured.NotOk(error) =>
        pprint.log(error)
        pprint.log(error.all)
        logger.error("foooo")
        error.all.foreach(message => logger.error(message))
        1
      case Configured.Ok(context) =>
        val markdown = Markdown.default(context)
        val runner = new MainOps(context.settings, markdown, logger)
        runner.run()
        if (context.logger.hasErrors) {
          1 // error
        } else {
          out.println(context.settings.out.toString)
          0
        }
    }
  }
}

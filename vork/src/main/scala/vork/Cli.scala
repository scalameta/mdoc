package vork

import java.io.PrintStream
import java.nio.file.Path
import metaconfig.Configured
import org.langmeta.internal.io.PathIO
import org.langmeta.io.AbsolutePath
import scala.tools.nsc.interpreter.OutputStream

object Cli {
  def main(args: Array[String]): Unit = {
    sys.exit(process(args, System.out, PathIO.workingDirectory.toNIO))
  }

  def process(
      args: Array[String],
      stdout: OutputStream,
      cwd: Path
  ): Int = {
    val out = new PrintStream(stdout)
    val logger = new Logger(out)
    val base = Args.default(AbsolutePath(cwd))
    Args.fromCliArgs(args.toList, logger, base) match {
      case Configured.NotOk(error) =>
        error.all.foreach(message => logger.error(message))
        1
      case Configured.Ok(context) =>
        val markdown = Markdown.default(context)
        val runner = new Processor(context.args, markdown, logger)
        runner.run()
        if (context.logger.hasErrors) {
          1 // error
        } else {
          out.println(context.args.out.toString)
          0
        }
    }
  }
}

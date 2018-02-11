package vork

import java.io.PrintStream
import scala.tools.nsc.interpreter.OutputStream
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.Configured

object Cli {
  def main(args: Array[String]): Unit = {
    sys.exit(process(args, System.out))
  }

  def process(args: Array[String], stdout: OutputStream = System.out): Int = {
    val out = new PrintStream(stdout)
    val logger = new Logger(stdout)
    Options.fromCliArgs(args.toList) match {
      case Configured.NotOk(error) =>
        error.all.foreach(message => logger.error(message))
        1
      case Configured.Ok(options) =>
        val context = Context.fromOptions(options, logger)
        val markdown = Markdown.default(context)
        val runner = new Processor(options, markdown, logger)
        runner.run()
        if (context.logger.hasErrors) {
          1 // error
        } else {
          out.println(options.out.toString)
          0
        }
    }
  }
}

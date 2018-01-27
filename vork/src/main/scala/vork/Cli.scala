package vork

import scala.util.control.NoStackTrace
import metaconfig.Conf
import metaconfig.ConfDecoder
import metaconfig.Configured

object Cli {
  def main(args: Array[String]): Unit = {
    Options.fromCliArgs(args.toList) match {
      case Configured.NotOk(error) =>
        throw new Exception(error.toString()) with NoStackTrace
      case Configured.Ok(options) =>
        val markdown = Markdown.default(options)
        val runner = new Processor(options, markdown, new Logger(System.out))
        runner.run()
        println(options.out.toString)
    }
  }
}
